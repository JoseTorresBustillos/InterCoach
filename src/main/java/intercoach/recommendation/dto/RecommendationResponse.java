package intercoach.recommendation.dto;

import intercoach.problem.model.Difficulty;

public record RecommendationResponse(
        Long problemId,
        String title,
        Difficulty difficulty,
        String category,
        String tags,
        String reason
) {}