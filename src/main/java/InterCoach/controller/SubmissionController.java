package InterCoach.controller;

import InterCoach.dto.SubmissionRequest;
import InterCoach.dto.SubmissionResponse;
import InterCoach.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/api/problems/{problemId}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse createSubmission(
            @PathVariable Long problemId,
            @Valid @RequestBody SubmissionRequest request
    ) {
        return submissionService.createSubmission(problemId, request);
    }

    @GetMapping("/api/submissions/{submissionId}")
    public SubmissionResponse getSubmissionById(@PathVariable Long submissionId) {
        return submissionService.getSubmissionById(submissionId);
    }

    @GetMapping("/api/problems/{problemId}/submissions")
    public List<SubmissionResponse> getSubmissionsForProblem(@PathVariable Long problemId) {
        return submissionService.getSubmissionsForProblem(problemId);
    }
}