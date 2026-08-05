package InterCoach.security;

import java.time.Instant;

public record JwtClaims(
        Long userId,
        String username,
        String email,
        String role,
        Instant expiresAt
) {
}
