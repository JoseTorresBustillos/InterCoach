package InterCoach.dto;

import InterCoach.model.Difficulty;

public record RecommendationResponse(
        Long problemId,
        String title,
        Difficulty difficulty,
        String category,
        String tags,
        String reason
) {}