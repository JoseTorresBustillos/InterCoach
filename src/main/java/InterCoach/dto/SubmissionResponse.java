package InterCoach.dto;

import InterCoach.model.SubmissionStatus;
import java.time.Instant;

public record SubmissionResponse(
        Long id,
        Long problemId,
        String submittedCode,
        String language,
        SubmissionStatus status,
        String aiFeedback,
        String timeComplexity,
        String spaceComplexity,
        Instant createdAt
) {}