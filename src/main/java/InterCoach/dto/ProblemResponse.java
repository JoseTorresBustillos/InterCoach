package InterCoach.dto;

import InterCoach.model.Difficulty;
import java.time.Instant;

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
) {}