package intercoach.dto;

import java.util.List;

public record CodeExecutionResponse(
        Long problemId,
        CodeExecutionStatus status,
        boolean allPassed,
        int passedTests,
        int totalTests,
        long durationMs,
        String compileOutput,
        List<CodeExecutionTestCaseResponse> testCases
) {}
