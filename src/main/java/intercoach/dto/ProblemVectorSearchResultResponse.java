package intercoach.dto;

import intercoach.model.Difficulty;

public record ProblemVectorSearchResultResponse(
        Long problemId,
        String title,
        Difficulty difficulty,
        String category,
        String tags,
        Double score,
        String excerpt
) {
}
