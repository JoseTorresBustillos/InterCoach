package intercoach.service;

import intercoach.dto.TestCaseRequest;
import intercoach.dto.TestCaseResponse;
import intercoach.exception.ResourceNotFoundException;
import intercoach.model.Problem;
import intercoach.model.TestCase;
import intercoach.repository.ProblemRepository;
import intercoach.repository.TestCaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<TestCaseResponse> getTestCasesForProblem(Long problemId) {
        if (!problemRepository.existsById(problemId)) {
            throw new ResourceNotFoundException(
                    "Problem not found with id: " + problemId
            );
        }

        return testCaseRepository.findByProblemId(problemId)
                .stream()
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
