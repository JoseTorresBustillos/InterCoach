package intercoach.dto;

import java.time.Instant;

/**
 * DTO returned to clients when sending test case data.
 */
public record TestCaseResponse(
        Long id,
        Long problemId,
        String input,
        String expectedOutput,
        boolean hidden,
        Instant createdAt
) {
}
