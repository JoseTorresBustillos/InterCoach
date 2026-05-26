package InterCoach.dto;

// DTO used when clients send test case creation requests.

import jakarta.validation.constraints.NotBlank;

public class TestCaseRequest {

    @NotBlank
    private String input;

    @NotBlank
    private String expectedOutput;

    private boolean hidden;

    public String getInput() { return input; }
    public String getExpectedOutput() { return expectedOutput; }
    public boolean isHidden() { return hidden; }
}