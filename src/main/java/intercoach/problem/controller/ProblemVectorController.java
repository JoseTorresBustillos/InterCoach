package intercoach.problem.controller;

import intercoach.problem.dto.ProblemVectorIndexResponse;
import intercoach.problem.dto.ProblemVectorSearchRequest;
import intercoach.problem.dto.ProblemVectorSearchResultResponse;
import intercoach.problem.service.ProblemVectorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
public class ProblemVectorController {

    private final ProblemVectorService problemVectorService;

    public ProblemVectorController(ProblemVectorService problemVectorService) {
        this.problemVectorService = problemVectorService;
    }

    @PostMapping("/vector-index")
    public ProblemVectorIndexResponse indexAllProblems() {
        return problemVectorService.indexAllProblems();
    }

    @PostMapping("/{problemId}/vector-index")
    public ProblemVectorIndexResponse indexProblem(@PathVariable Long problemId) {
        return problemVectorService.indexProblem(problemId);
    }

    @PostMapping("/semantic-search")
    public List<ProblemVectorSearchResultResponse> searchProblems(
            @Valid @RequestBody ProblemVectorSearchRequest request
    ) {
        return problemVectorService.searchProblems(request);
    }
}
