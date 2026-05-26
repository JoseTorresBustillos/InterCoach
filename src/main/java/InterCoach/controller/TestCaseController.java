package InterCoach.controller;

// Handles HTTP requests related to problem test cases.

import InterCoach.dto.TestCaseRequest;
import InterCoach.dto.TestCaseResponse;
import InterCoach.service.TestCaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems/{problemId}/test-cases")
public class TestCaseController {

    private final TestCaseService testCaseService;

    
    // Constructor injection keeps dependencies explicit and testable.
public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseResponse createTestCase(
            @PathVariable Long problemId,
            @Valid @RequestBody TestCaseRequest request
    ) {
        return testCaseService.createTestCase(problemId, request);
    }

    @GetMapping
    public List<TestCaseResponse> getTestCasesForProblem(@PathVariable Long problemId) {
        return testCaseService.getTestCasesForProblem(problemId);
    }
}