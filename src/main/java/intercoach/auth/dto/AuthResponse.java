package intercoach.auth.dto;

import intercoach.user.dto.UserResponse;
import java.time.Instant;

public record AuthResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UserResponse user
) {
}
