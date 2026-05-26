package InterCoach.service;

// Contains business logic for creating, retrieving, updating, and deleting problems.

import InterCoach.dto.ProblemRequest;
import InterCoach.dto.ProblemResponse;
import InterCoach.model.Problem;
import InterCoach.repository.ProblemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProblemService {

    private final ProblemRepository problemRepository;

    
    // Constructor injection keeps dependencies explicit and testable.
public ProblemService(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    public List<ProblemResponse> getAllProblems() {
        return problemRepository.findAll()
                .stream()
                .                // Converts entities into response DTOs before returning them.
map(this::toResponse)
                .toList();
    }

    public ProblemResponse getProblemById(Long id) {
        Problem problem = problemRepository.findById(id)
                .                // Throws an error if the requested record does not exist.
orElseThrow(() -> new RuntimeException("Problem not found with id: " + id));

        return toResponse(problem);
    }

    public ProblemResponse createProblem(ProblemRequest request) {
        Problem problem = new Problem();
        updateProblemFields(problem, request);

        Problem savedProblem =         // Persists the problem and returns the saved entity.
problemRepository.save(problem);
        return toResponse(savedProblem);
    }

    public ProblemResponse updateProblem(Long id, ProblemRequest request) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found with id: " + id));

        updateProblemFields(problem, request);

        Problem savedProblem = problemRepository.save(problem);
        return toResponse(savedProblem);
    }

    public void deleteProblem(Long id) {
        if (!problemRepository.existsById(id)) {
            throw new RuntimeException("Problem not found with id: " + id);
        }

        problemRepository.deleteById(id);
    }

    private void updateProblemFields(Problem problem, ProblemRequest request) {
        problem.setTitle(request.getTitle());
        problem.setDescription(request.getDescription());
        problem.setDifficulty(request.getDifficulty());
        problem.setCategory(request.getCategory());
        problem.setTags(request.getTags());
        problem.setExamples(request.getExamples());
        problem.setConstraints(request.getConstraints());
        problem.setStarterCode(request.getStarterCode());
        problem.setSolutionExplanation(request.getSolutionExplanation());
    }

    private ProblemResponse toResponse(Problem problem) {
        return new ProblemResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getDescription(),
                problem.getDifficulty(),
                problem.getCategory(),
                problem.getTags(),
                problem.getExamples(),
                problem.getConstraints(),
                problem.getStarterCode(),
                problem.getSolutionExplanation(),
                problem.getCreatedAt(),
                problem.getUpdatedAt()
        );
    }
}