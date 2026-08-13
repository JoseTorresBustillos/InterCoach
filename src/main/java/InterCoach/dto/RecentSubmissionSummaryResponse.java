package InterCoach.dto;

import InterCoach.model.Difficulty;
import InterCoach.model.SubmissionStatus;

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
