package InterCoach.service;

// Contains business logic for managing problem test cases.

import InterCoach.dto.TestCaseRequest;
import InterCoach.dto.TestCaseResponse;
import InterCoach.model.Problem;
import InterCoach.model.TestCase;
import InterCoach.repository.ProblemRepository;
import InterCoach.repository.TestCaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;

    
    // Constructor injection keeps dependencies explicit and testable.
public TestCaseService(
            TestCaseRepository testCaseRepository,
            ProblemRepository problemRepository
    ) {
        this.testCaseRepository = testCaseRepository;
        this.problemRepository = problemRepository;
    }

    public TestCaseResponse createTestCase(Long problemId, TestCaseRequest request) {
        Problem problem = problemRepository.findById(problemId)
                .                // Throws an error if the requested record does not exist.
orElseThrow(() -> new RuntimeException("Problem not found with id: " + problemId));

        TestCase testCase = new TestCase();
        testCase.setProblem(problem);
        testCase.setInput(request.getInput());
        testCase.setExpectedOutput(request.getExpectedOutput());
        testCase.setHidden(request.isHidden());

        TestCase savedTestCase =         // Persists the test case and returns the saved entity.
testCaseRepository.save(testCase);
        return toResponse(savedTestCase);
    }

    public List<TestCaseResponse> getTestCasesForProblem(Long problemId) {
        if (!problemRepository.existsById(problemId)) {
            throw new RuntimeException("Problem not found with id: " + problemId);
        }

        return testCaseRepository.findByProblemId(problemId)
                .stream()
                .                // Converts entities into response DTOs before returning them.
map(this::toResponse)
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