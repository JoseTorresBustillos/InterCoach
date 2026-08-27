package intercoach.controller;

import intercoach.dto.TestCaseRequest;
import intercoach.dto.TestCaseResponse;
import intercoach.security.UserAccessService;
import intercoach.service.TestCaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Handles HTTP requests related to problem test cases.
 */
@RestController
@RequestMapping("/api/problems/{problemId}/test-cases")
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final UserAccessService userAccessService;

    public TestCaseController(
            TestCaseService testCaseService,
            UserAccessService userAccessService
    ) {
        this.testCaseService = testCaseService;
        this.userAccessService = userAccessService;
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
    public List<TestCaseResponse> getTestCasesForProblem(
            @PathVariable Long problemId,
            Authentication authentication
    ) {
        return testCaseService.getTestCasesForProblem(
                problemId,
                userAccessService.isAdmin(authentication)
        );
    }
}
