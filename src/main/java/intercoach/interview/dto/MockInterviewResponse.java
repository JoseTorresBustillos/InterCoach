package intercoach.interview.dto;

import intercoach.interview.model.InterviewStatus;
import intercoach.problem.model.Difficulty;
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