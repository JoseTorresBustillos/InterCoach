package intercoach.dto;

import intercoach.model.Difficulty;
import intercoach.model.InterviewStatus;

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
