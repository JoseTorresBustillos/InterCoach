package InterCoach.dto;

import java.util.List;

public record AiFeedbackResponse(
        String summary,
        String correctness,
        List<String> bugs,
        List<String> edgeCases,
        String timeComplexity,
        String spaceComplexity,
        String hint,
        String suggestedImprovement
) {}