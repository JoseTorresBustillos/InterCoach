package intercoach.progress.dto;

import intercoach.interview.model.InterviewStatus;
import intercoach.problem.model.Difficulty;
import java.time.Instant;

public record RecentMockInterviewSummaryResponse(
        Long sessionId,
        Long problemId,
        String problemTitle,
        Difficulty difficulty,
        InterviewStatus status,
        Integer durationMinutes,
        Instant startedAt,
        Instant completedAt
) {
}
