package InterCoach.dto;

// DTO returned to clients when sending test case data.

import java.time.Instant;

public record TestCaseResponse(
        Long id,
        Long problemId,
        String input,
        String expectedOutput,
        boolean hidden,
        Instant createdAt
) {}