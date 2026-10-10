package intercoach.submission.controller;

import intercoach.auth.security.UserAccessService;
import intercoach.submission.dto.SubmissionRequest;
import intercoach.submission.dto.SubmissionResponse;
import intercoach.submission.service.QueuedSubmissionService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "intercoach.review-queue.enabled", havingValue = "true")
public class QueuedSubmissionController {

    private final QueuedSubmissionService submissions;
    private final UserAccessService access;

    public QueuedSubmissionController(QueuedSubmissionService submissions, UserAccessService access) {
        this.submissions = submissions;
        this.access = access;
    }

    @PostMapping("/api/problems/{problemId}/submissions/queued")
    public ResponseEntity<SubmissionResponse> createQueuedSubmission(
            @PathVariable Long problemId,
            @Valid @RequestBody SubmissionRequest request,
            Authentication authentication
    ) {
        access.assertCanAccessUser(request.getUserId(), authentication);
        SubmissionResponse response = submissions.createQueuedSubmission(problemId, request);
        return ResponseEntity.accepted()
                .location(URI.create("/api/submissions/" + response.id()))
                .body(response);
    }
}
