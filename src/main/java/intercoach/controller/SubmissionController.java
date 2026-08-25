package intercoach.controller;

import intercoach.dto.SubmissionRequest;
import intercoach.dto.SubmissionResponse;
import intercoach.security.UserAccessService;
import intercoach.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
