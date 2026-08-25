package intercoach.dto;

import java.util.List;

public record StudyAssistantResponse(
        String answer,
        List<StudyAssistantCitationResponse> citations
) {}
