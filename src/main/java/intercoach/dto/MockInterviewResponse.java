package intercoach.dto;

import intercoach.model.Difficulty;
import intercoach.model.InterviewStatus;

import java.time.Instant;

public record MockInterviewResponse(
        Long sessionId,
        Long userId,
        Long problemId,
        String problemTitle,
        Difficulty difficulty,
        String category,
        String description,
        String constraints,
        String examples,
        String starterCode,
        InterviewStatus status,
        Integer durationMinutes,
        Instant startedAt,
        Instant completedAt
) {
}