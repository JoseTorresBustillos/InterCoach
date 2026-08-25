package intercoach.controller;

import intercoach.dto.CodeExecutionRequest;
import intercoach.dto.CodeExecutionResponse;
import intercoach.service.CodeExecutionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
