package intercoach.submission.controller;

import intercoach.auth.security.UserAccessService;
import intercoach.submission.dto.SubmissionRequest;
import intercoach.submission.dto.SubmissionResponse;
import intercoach.submission.service.SubmissionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubmissionController {

    private final SubmissionService submissionService;
    private final UserAccessService userAccessService;

    public SubmissionController(
            SubmissionService submissionService,
            UserAccessService userAccessService
    ) {
        this.submissionService = submissionService;
        this.userAccessService = userAccessService;
    }

    @PostMapping("/api/problems/{problemId}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse createSubmission(
            @PathVariable Long problemId,
            @Valid @RequestBody SubmissionRequest request,
            Authentication authentication
    ) {
        userAccessService.assertCanAccessUser(request.getUserId(), authentication);

        return submissionService.createSubmission(problemId, request);
    }

    @GetMapping("/api/submissions/{submissionId}")
    public SubmissionResponse getSubmissionById(
            @PathVariable Long submissionId,
            Authentication authentication
    ) {
        userAccessService.assertCanAccessSubmission(
                submissionId,
                authentication
        );

        return submissionService.getSubmissionById(submissionId);
    }

    @GetMapping("/api/problems/{problemId}/submissions")
    public List<SubmissionResponse> getSubmissionsForProblem(
            @PathVariable Long problemId,
            Authentication authentication
    ) {
        userAccessService.assertAdmin(authentication);

        return submissionService.getSubmissionsForProblem(problemId);
    }

    @GetMapping("/api/users/{userId}/submissions")
    public List<SubmissionResponse> getSubmissionsForUser(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        userAccessService.assertCanAccessUser(userId, authentication);

        return submissionService.getSubmissionsForUser(userId);
    }
}
