package intercoach.assistant.dto;

import intercoach.problem.model.Difficulty;
import intercoach.submission.model.SubmissionStatus;
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
