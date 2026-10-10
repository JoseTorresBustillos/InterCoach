package intercoach.submission.service;

import intercoach.submission.dto.SubmissionRequest;
import intercoach.submission.dto.SubmissionResponse;
import intercoach.submission.model.Submission;
import intercoach.submission.repository.ReviewJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueuedSubmissionService {

    private final SubmissionService submissions;
    private final ReviewJobRepository jobs;

    public QueuedSubmissionService(SubmissionService submissions, ReviewJobRepository jobs) {
        this.submissions = submissions;
        this.jobs = jobs;
    }

    @Transactional
    public SubmissionResponse createQueuedSubmission(Long problemId, SubmissionRequest request) {
        Submission submission = submissions.createPendingSubmission(problemId, request);
        jobs.enqueue(submission.getId());
        return submissions.toResponse(submission);
    }
}
