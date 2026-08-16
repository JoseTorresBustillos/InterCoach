package InterCoach.dto;

import jakarta.validation.constraints.NotBlank;

public class CodeExecutionRequest {

    @NotBlank
    private String submittedCode;

    private String language = "Java";

    public String getSubmittedCode() {
        return submittedCode;
    }

    public String getLanguage() {
        return language == null || language.isBlank() ? "Java" : language;
    }
}
