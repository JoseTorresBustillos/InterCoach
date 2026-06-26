package InterCoach.dto;

import jakarta.validation.constraints.NotBlank;

public class StudyAssistantRequest {

    @NotBlank
    private String question;

    public String getQuestion() {
        return question;
    }
}