package InterCoach.service;

import InterCoach.dto.AuthResponse;
import InterCoach.dto.LoginRequest;
import InterCoach.dto.RegisterRequest;
import InterCoach.exception.AuthenticationFailedException;
import InterCoach.exception.DuplicateResourceException;
import InterCoach.model.AppUser;
import InterCoach.repository.AppUserRepository;
import InterCoach.security.JwtService;
import InterCoach.security.JwtToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                appUserRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void registerHashesPasswordAndReturnsJwt() {
        RegisterRequest request = registerRequest();
        JwtToken token = new JwtToken("signed.jwt.token", Instant.now().plusSeconds(3600));

        given(appUserRepository.existsByUsernameIgnoreCase("coder"))
                .willReturn(false);
        given(appUserRepository.existsByEmailIgnoreCase("coder@example.com"))
                .willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("hashed-password");
        given(appUserRepository.save(any(AppUser.class)))
                .willAnswer(invocation -> savedUser(invocation.getArgument(0)));
        given(jwtService.generateToken(any(AppUser.class))).willReturn(token);

        AuthResponse response = authService.register(request);

        ArgumentCaptor<AppUser> userCaptor =
                ArgumentCaptor.forClass(AppUser.class);
        then(appUserRepository).should().save(userCaptor.capture());

        assertThat(userCaptor.getValue().getPasswordHash())
                .isEqualTo("hashed-password");
        assertThat(userCaptor.getValue().getRole()).isEqualTo("USER");
        assertThat(response.token()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().username()).isEqualTo("coder");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegisterRequest request = registerRequest();
        given(appUserRepository.existsByUsernameIgnoreCase("coder"))
                .willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username is already in use.");
    }

    @Test
    void loginAcceptsUsernameOrEmailAndReturnsJwt() {
        LoginRequest request = loginRequest("coder@example.com");
        AppUser user = existingUser();
        JwtToken token = new JwtToken("signed.jwt.token", Instant.now().plusSeconds(3600));

        given(appUserRepository.findByUsernameIgnoreCase("coder@example.com"))
                .willReturn(Optional.empty());
        given(appUserRepository.findByEmailIgnoreCase("coder@example.com"))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "hashed-password"))
                .willReturn(true);
        given(jwtService.generateToken(user)).willReturn(token);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("signed.jwt.token");
        assertThat(response.user().email()).isEqualTo("coder@example.com");
    }

    @Test
    void loginRejectsBadPassword() {
        LoginRequest request = loginRequest("coder");
        AppUser user = existingUser();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "hashed-password"))
                .willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid username/email or password.");
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        ReflectionTestUtils.setField(request, "username", "coder");
        ReflectionTestUtils.setField(request, "email", "coder@example.com");
        ReflectionTestUtils.setField(request, "password", "password123");
        return request;
    }

    private LoginRequest loginRequest(String usernameOrEmail) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "usernameOrEmail", usernameOrEmail);
        ReflectionTestUtils.setField(request, "password", "password123");
        return request;
    }

    private AppUser existingUser() {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 42L);
        user.setUsername("coder");
        user.setEmail("coder@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole("USER");
        return user;
    }

    private AppUser savedUser(AppUser user) {
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }
}
