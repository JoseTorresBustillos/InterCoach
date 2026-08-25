package intercoach.dto;

import intercoach.model.Difficulty;
import intercoach.model.SubmissionStatus;

import java.time.Instant;

public record RecentSubmissionSummaryResponse(
        Long submissionId,
        Long problemId,
        String problemTitle,
        Difficulty difficulty,
        String category,
        SubmissionStatus status,
        Instant createdAt
) {
}
