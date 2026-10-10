package intercoach.submission.service;

import intercoach.problem.model.Problem;
import intercoach.submission.config.ReviewQueueProperties;
import intercoach.submission.dto.AiFeedbackResponse;
import intercoach.submission.model.ReviewJobClaim;
import intercoach.submission.model.Submission;
import intercoach.submission.repository.ReviewJobRepository;
import intercoach.submission.repository.SubmissionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewJobWorkerTest {

    @Mock private ReviewJobRepository jobs;
    @Mock private SubmissionRepository submissions;
    @Mock private QueuedAiFeedbackService ai;
    @Mock private ReviewJobCompletionService completion;

    private ReviewJobWorker worker;
    private final ReviewJobClaim claim = new ReviewJobClaim(10L, UUID.randomUUID(), 1);
    private final AiFeedbackResponse feedback = new AiFeedbackResponse(
            "Good", "Correct", List.of(), List.of(), "O(n)", "O(1)", "Hint", "Improve"
    );
    private Problem problem;

    @BeforeEach
    void setUp() {
        worker = new ReviewJobWorker(jobs, submissions, ai, completion, new ReviewQueueProperties());
        problem = new Problem();
    }

    @Test
    void emptyQueueDoesNotCallProvider() {
        when(jobs.claim(3, 300)).thenReturn(Optional.empty());
        worker.runOnce();
        verify(jobs).failExpiredExhaustedJobs(3);
        verifyNoInteractions(ai, completion, submissions);
    }

    @Test
    void successfulReviewCompletesClaim() {
        prepareClaim();
        when(ai.reviewSubmission(problem, "code")).thenReturn(feedback);
        when(completion.complete(claim, feedback)).thenReturn(true);
        worker.runOnce();
        verify(completion).complete(claim, feedback);
    }

    @Test
    void providerFailureSchedulesRetry() {
        prepareClaim();
        when(ai.reviewSubmission(problem, "code")).thenThrow(new IllegalStateException("unavailable"));
        worker.runOnce();
        verify(completion).fail(claim);
    }

    @Test
    void nullFeedbackSchedulesRetry() {
        prepareClaim();
        worker.runOnce();
        verify(completion).fail(claim);
    }

    @Test
    void expiredResultIsDiscardedWithoutFailingNewerClaim() {
        prepareClaim();
        when(ai.reviewSubmission(problem, "code")).thenReturn(feedback);
        when(completion.complete(claim, feedback)).thenReturn(false);
        worker.runOnce();
        verify(completion).complete(claim, feedback);
        verify(completion, org.mockito.Mockito.never()).fail(claim);
    }

    @Test
    void databaseFailureDoesNotEscapeScheduler() {
        when(jobs.claim(3, 300)).thenThrow(new IllegalStateException("database unavailable"));
        assertThatCode(worker::runOnce).doesNotThrowAnyException();
        verifyNoInteractions(ai, completion);
    }

    private void prepareClaim() {
        when(jobs.claim(3, 300)).thenReturn(Optional.of(claim));
        Submission submission = new Submission();
        submission.setProblem(problem);
        submission.setSubmittedCode("code");
        when(submissions.findForReviewById(10L)).thenReturn(Optional.of(submission));
    }
}
