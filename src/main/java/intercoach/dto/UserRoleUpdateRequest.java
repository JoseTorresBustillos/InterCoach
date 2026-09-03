package intercoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRoleUpdateRequest(
        @NotBlank
        @Pattern(regexp = "(?i)USER|ADMIN")
        String role
) {
}
