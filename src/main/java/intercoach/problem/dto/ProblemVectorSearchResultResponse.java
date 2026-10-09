package intercoach.problem.dto;

import intercoach.problem.model.Difficulty;

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
