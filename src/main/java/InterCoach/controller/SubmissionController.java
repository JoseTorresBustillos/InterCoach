package InterCoach.controller;

import InterCoach.dto.SubmissionRequest;
import InterCoach.dto.SubmissionResponse;
import InterCoach.security.UserAccessService;
import InterCoach.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
