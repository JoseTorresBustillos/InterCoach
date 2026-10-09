package intercoach.progress.dto;

import intercoach.problem.model.Difficulty;
import intercoach.submission.model.SubmissionStatus;
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
