package intercoach.dto;

import intercoach.model.Difficulty;

import java.time.Instant;

/**
 * DTO returned to clients when sending problem data.
 */
public record ProblemResponse(
        Long id,
        String title,
        String description,
        Difficulty difficulty,
        String category,
        String tags,
        String examples,
        String constraints,
        String starterCode,
        String solutionExplanation,
        Instant createdAt,
        Instant updatedAt
) {
}
