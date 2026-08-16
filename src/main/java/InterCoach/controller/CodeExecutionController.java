package InterCoach.controller;

import InterCoach.dto.CodeExecutionRequest;
import InterCoach.dto.CodeExecutionResponse;
import InterCoach.service.CodeExecutionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problems/{problemId}/run")
public class CodeExecutionController {

    private final CodeExecutionService codeExecutionService;

    public CodeExecutionController(CodeExecutionService codeExecutionService) {
        this.codeExecutionService = codeExecutionService;
    }

    @PostMapping
    public CodeExecutionResponse runCode(
            @PathVariable Long problemId,
            @Valid @RequestBody CodeExecutionRequest request
    ) {
        return codeExecutionService.runCode(problemId, request);
    }
}
