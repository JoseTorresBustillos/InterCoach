package InterCoach.controller;

// Handles HTTP requests related to coding problems.

import InterCoach.dto.ProblemRequest;
import InterCoach.dto.ProblemResponse;
import InterCoach.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    
    // Constructor injection keeps dependencies explicit and testable.
public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    public List<ProblemResponse> getAllProblems() {
        return problemService.getAllProblems();
    }

    @GetMapping("/{id}")
    public ProblemResponse getProblemById(@PathVariable Long id) {
        return problemService.getProblemById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemResponse createProblem(@Valid @RequestBody ProblemRequest request) {
        return problemService.createProblem(request);
    }

    @PutMapping("/{id}")
    public ProblemResponse updateProblem(
            @PathVariable Long id,
            @Valid @RequestBody ProblemRequest request
    ) {
        return problemService.updateProblem(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProblem(@PathVariable Long id) {
        problemService.deleteProblem(id);
    }
}