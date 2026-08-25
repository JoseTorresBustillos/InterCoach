package intercoach.dto;

import intercoach.model.Difficulty;
import intercoach.model.SubmissionStatus;

import java.time.Instant;

public record StudyAssistantCitationResponse(
        String label,
        String type,
        Long problemId,
        String title,
        Difficulty difficulty,
        String category,
        String tags,
        Double score,
        SubmissionStatus submissionStatus,
        Instant submittedAt,
        String excerpt
) {
}
