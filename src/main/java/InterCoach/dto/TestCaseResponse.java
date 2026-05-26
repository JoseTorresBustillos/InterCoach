package InterCoach.dto;

import java.time.Instant;

public record TestCaseResponse(
        Long id,
        Long problemId,
        String input,
        String expectedOutput,
        boolean hidden,
        Instant createdAt
) {}