package intercoach.submission.service;

import intercoach.shared.exception.ResourceNotFoundException;
import intercoach.submission.config.ReviewQueueProperties;
import intercoach.submission.dto.AiFeedbackResponse;
import intercoach.submission.model.ReviewJobClaim;
import intercoach.submission.model.Submission;
import intercoach.submission.model.SubmissionStatus;
import intercoach.submission.repository.ReviewJobRepository;
import intercoach.submission.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewJobCompletionService {

    private final ReviewJobRepository jobs;
    private final SubmissionRepository submissions;
    private final ReviewQueueProperties properties;

    public ReviewJobCompletionService(
            ReviewJobRepository jobs,
            SubmissionRepository submissions,
            ReviewQueueProperties properties
    ) {
        this.jobs = jobs;
        this.submissions = submissions;
        this.properties = properties;
    }

    @Transactional
    public boolean complete(ReviewJobClaim claim, AiFeedbackResponse feedback) {
        if (!jobs.complete(claim)) {
            return false;
        }
        Submission submission = submission(claim);
        SubmissionService.applyFeedback(submission, feedback);
        submissions.saveAndFlush(submission);
        return true;
    }

    @Transactional
    public void fail(ReviewJobClaim claim) {
        boolean exhausted = claim.attemptCount() >= properties.getMaxAttempts();
        if (jobs.fail(claim, exhausted, properties.getRetryDelaySeconds()) && exhausted) {
            Submission submission = submission(claim);
            submission.setStatus(SubmissionStatus.FAILED);
            submission.setAiFeedback(ReviewJobRepository.FAILURE_MESSAGE);
            submissions.saveAndFlush(submission);
        }
    }

    private Submission submission(ReviewJobClaim claim) {
        return submissions.findById(claim.submissionId()).orElseThrow(() ->
                new ResourceNotFoundException("Submission no longer exists: " + claim.submissionId())
        );
    }
}
