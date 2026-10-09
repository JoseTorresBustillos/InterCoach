package intercoach.problem.service;

import intercoach.problem.dto.TestCaseRequest;
import intercoach.problem.dto.TestCaseResponse;
import intercoach.problem.model.Problem;
import intercoach.problem.model.TestCase;
import intercoach.problem.repository.ProblemRepository;
import intercoach.problem.repository.TestCaseRepository;
import intercoach.shared.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Contains business logic for managing problem test cases.
 */
@Service
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;

    public TestCaseService(
            TestCaseRepository testCaseRepository,
            ProblemRepository problemRepository
    ) {
        this.testCaseRepository = testCaseRepository;
        this.problemRepository = problemRepository;
    }

    public TestCaseResponse createTestCase(Long problemId, TestCaseRequest request) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Problem not found with id: " + problemId
                ));

        TestCase testCase = new TestCase();
        testCase.setProblem(problem);
        testCase.setInput(request.getInput());
        testCase.setExpectedOutput(request.getExpectedOutput());
        testCase.setHidden(request.isHidden());

        TestCase savedTestCase = testCaseRepository.save(testCase);
        return toResponse(savedTestCase);
    }

    public List<TestCaseResponse> getTestCasesForProblem(
            Long problemId,
            boolean includeHidden
    ) {
        if (!problemRepository.existsById(problemId)) {
            throw new ResourceNotFoundException(
                    "Problem not found with id: " + problemId
            );
        }

        return testCaseRepository.findByProblemId(problemId)
                .stream()
                .filter(testCase -> includeHidden || !testCase.isHidden())
                .map(this::toResponse)
                .toList();
    }

    private TestCaseResponse toResponse(TestCase testCase) {
        return new TestCaseResponse(
                testCase.getId(),
                testCase.getProblem().getId(),
                testCase.getInput(),
                testCase.getExpectedOutput(),
                testCase.isHidden(),
                testCase.getCreatedAt()
        );
    }
}
