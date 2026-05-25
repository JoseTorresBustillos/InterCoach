package InterCoach.dto;

import InterCoach.model.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProblemRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private Difficulty difficulty;

    private String category;
    private String tags;
    private String examples;
    private String constraints;
    private String starterCode;
    private String solutionExplanation;

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getCategory() { return category; }
    public String getTags() { return tags; }
    public String getExamples() { return examples; }
    public String getConstraints() { return constraints; }
    public String getStarterCode() { return starterCode; }
    public String getSolutionExplanation() { return solutionExplanation; }
}