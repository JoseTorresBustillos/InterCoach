package intercoach.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull Boolean active
) {
}
