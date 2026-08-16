package InterCoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CodeExecutionRequest {

    @NotBlank
    @Size(max = 20000)
    private String submittedCode;

    private String language = "Java";

    public String getSubmittedCode() {
        return submittedCode;
    }

    public String getLanguage() {
        return language == null || language.isBlank() ? "Java" : language;
    }
}
