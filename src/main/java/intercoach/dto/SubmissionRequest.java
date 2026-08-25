package intercoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SubmissionRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String submittedCode;

    private String language = "Java";

    public Long getUserId() {
        return userId;
    }

    public String getSubmittedCode() {
        return submittedCode;
    }

    public String getLanguage() {
        return language;
    }
}