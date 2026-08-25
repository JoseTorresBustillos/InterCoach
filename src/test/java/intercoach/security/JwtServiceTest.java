package intercoach.security;

import intercoach.model.AppUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new ObjectMapper(),
            "test-secret-with-at-least-thirty-two-characters",
            60
    );

    @Test
    void generatedTokenCanBeValidated() {
        AppUser user = user();

        JwtToken token = jwtService.generateToken(user);
        JwtClaims claims = jwtService.validateToken(token.value());

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.username()).isEqualTo("coder");
        assertThat(claims.email()).isEqualTo("coder@example.com");
        assertThat(claims.role()).isEqualTo("USER");
        assertThat(claims.expiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtToken token = jwtService.generateToken(user());
        String tamperedToken = token.value().substring(0, token.value().length() - 2)
                + "xx";

        assertThatThrownBy(() -> jwtService.validateToken(tamperedToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid JWT signature.");
    }

    private AppUser user() {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 42L);
        user.setUsername("coder");
        user.setEmail("coder@example.com");
        user.setRole("USER");
        return user;
    }
}
