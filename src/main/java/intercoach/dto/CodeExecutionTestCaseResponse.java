package intercoach.dto;

public record CodeExecutionTestCaseResponse(
        Long testCaseId,
        String input,
        String expectedOutput,
        String actualOutput,
        CodeExecutionTestCaseStatus status,
        boolean passed,
        long durationMs,
        String errorOutput
) {}
