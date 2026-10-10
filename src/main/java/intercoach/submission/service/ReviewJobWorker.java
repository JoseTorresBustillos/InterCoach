package intercoach.submission.service;

import intercoach.submission.config.ReviewQueueProperties;
import intercoach.submission.dto.AiFeedbackResponse;
import intercoach.submission.model.ReviewJobClaim;
import intercoach.submission.model.Submission;
import intercoach.submission.repository.ReviewJobRepository;
import intercoach.submission.repository.SubmissionRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "intercoach.review-queue.enabled", havingValue = "true")
public class ReviewJobWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewJobWorker.class);

    private final ReviewJobRepository jobs;
    private final SubmissionRepository submissions;
    private final AiFeedbackService feedbackService;
    private final ReviewJobCompletionService completion;
    private final ReviewQueueProperties properties;

    public ReviewJobWorker(
            ReviewJobRepository jobs,
            SubmissionRepository submissions,
            AiFeedbackService feedbackService,
            ReviewJobCompletionService completion,
            ReviewQueueProperties properties
    ) {
        this.jobs = jobs;
        this.submissions = submissions;
        this.feedbackService = feedbackService;
        this.completion = completion;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${intercoach.review-queue.poll-interval-ms:5000}")
    public void runOnce() {
        try {
            jobs.failExpiredExhaustedJobs(properties.getMaxAttempts());
            jobs.claim(properties.getMaxAttempts(), properties.getLeaseSeconds())
                    .ifPresent(this::review);
        } catch (RuntimeException exception) {
            LOGGER.error("Background review polling failed; leased work remains recoverable", exception);
        }
    }

    private void review(ReviewJobClaim claim) {
        try {
            // The repository fetches problem context before the network call, without holding locks.
            Optional<Submission> found = submissions.findForReviewById(claim.submissionId());
            Submission submission = found.orElseThrow(() ->
                    new IllegalStateException("Queued submission no longer exists")
            );
            AiFeedbackResponse feedback = feedbackService.reviewSubmission(
                    submission.getProblem(), submission.getSubmittedCode()
            );
            if (feedback == null) {
                throw new IllegalStateException("AI provider returned no feedback");
            }
            if (!completion.complete(claim, feedback)) {
                LOGGER.info("Discarded expired review result for submission {}", claim.submissionId());
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("Background review failed for submission {}, attempt {}",
                    claim.submissionId(), claim.attemptCount(), exception);
            completion.fail(claim);
        }
    }
}
