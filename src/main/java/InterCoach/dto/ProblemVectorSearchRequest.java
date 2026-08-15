package InterCoach.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ProblemVectorSearchRequest {

    @NotBlank
    private String query;

    @Min(1)
    @Max(20)
    private Integer topK = 5;

    public String getQuery() {
        return query;
    }

    public Integer getTopK() {
        return topK;
    }
}
