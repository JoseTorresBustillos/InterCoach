package intercoach.dto;

import intercoach.model.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class MockInterviewRequest {

    @NotNull
    private Difficulty difficulty;

    @Min(10)
    @Max(180)
    private Integer durationMinutes = 45;

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }
}