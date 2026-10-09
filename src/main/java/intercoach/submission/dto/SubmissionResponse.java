package intercoach.submission.dto;

import intercoach.submission.model.SubmissionStatus;
import java.time.Instant;

public record SubmissionResponse(
        Long id,
        Long problemId,
        Long userId,
        String submittedCode,
        String language,
        SubmissionStatus status,
        String aiFeedback,
        String feedbackSummary,
        String correctness,
        String bugs,
        String edgeCases,
        String timeComplexity,
        String spaceComplexity,
        String hint,
        String suggestedImprovement,
        Instant createdAt
) {
}