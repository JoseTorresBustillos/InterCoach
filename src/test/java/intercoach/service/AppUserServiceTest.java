package intercoach.service;

import intercoach.dto.AuthResponse;
import intercoach.dto.PasswordChangeRequest;
import intercoach.dto.UserProfileUpdateRequest;
import intercoach.dto.UserResponse;
import intercoach.exception.AuthenticationFailedException;
import intercoach.exception.DuplicateResourceException;
import intercoach.model.AppUser;
import intercoach.repository.AppUserRepository;
import intercoach.security.JwtService;
import intercoach.security.JwtToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserService = new AppUserService(
                appUserRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void updateCurrentUserProfileNormalizesFieldsAndRefreshesToken() {
        AppUser user = existingUser();
        UserProfileUpdateRequest request =
                profileRequest(" learner ", "Learner@Example.COM ");
        JwtToken token = new JwtToken(
                "fresh.jwt.token",
                Instant.now().plusSeconds(3600)
        );

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(appUserRepository.existsByUsernameIgnoreCase("learner"))
                .willReturn(false);
        given(appUserRepository.existsByEmailIgnoreCase("learner@example.com"))
                .willReturn(false);
        given(appUserRepository.save(user)).willReturn(user);
        given(jwtService.generateToken(user)).willReturn(token);

        AuthResponse response =
                appUserService.updateCurrentUserProfile("coder", request);

        assertThat(response.token()).isEqualTo("fresh.jwt.token");
        assertThat(response.user().username()).isEqualTo("learner");
        assertThat(response.user().email()).isEqualTo("learner@example.com");
        then(appUserRepository).should().save(user);
    }

    @Test
    void updateCurrentUserProfileRejectsDuplicateUsername() {
        AppUser user = existingUser();
        UserProfileUpdateRequest request =
                profileRequest("mentor", null);

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(appUserRepository.existsByUsernameIgnoreCase("mentor"))
                .willReturn(true);

        assertThatThrownBy(() ->
                appUserService.updateCurrentUserProfile("coder", request)
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username is already in use.");

        then(appUserRepository).should(never()).save(user);
    }

    @Test
    void updateCurrentUserProfileRejectsEmptyPatch() {
        AppUser user = existingUser();
        UserProfileUpdateRequest request = profileRequest(null, null);

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));

        assertThatThrownBy(() ->
                appUserService.updateCurrentUserProfile("coder", request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one profile field must be provided.");
    }

    @Test
    void updateCurrentUserProfileRejectsTrimmedShortUsername() {
        AppUser user = existingUser();
        UserProfileUpdateRequest request = profileRequest(" ab ", null);

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));

        assertThatThrownBy(() ->
                appUserService.updateCurrentUserProfile("coder", request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username must be between 3 and 50 characters.");

        then(appUserRepository).should(never()).save(user);
    }

    @Test
    void changeCurrentUserPasswordVerifiesCurrentPasswordAndSavesNewHash() {
        AppUser user = existingUser();
        PasswordChangeRequest request =
                passwordChangeRequest("password123", "newpassword123");

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "hashed-password"))
                .willReturn(true);
        given(passwordEncoder.encode("newpassword123"))
                .willReturn("new-hashed-password");

        appUserService.changeCurrentUserPassword("coder", request);

        ArgumentCaptor<AppUser> userCaptor =
                ArgumentCaptor.forClass(AppUser.class);
        then(appUserRepository).should().save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash())
                .isEqualTo("new-hashed-password");
    }

    @Test
    void changeCurrentUserPasswordRejectsBadCurrentPassword() {
        AppUser user = existingUser();
        PasswordChangeRequest request =
                passwordChangeRequest("wrong-password", "newpassword123");

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "hashed-password"))
                .willReturn(false);

        assertThatThrownBy(() ->
                appUserService.changeCurrentUserPassword("coder", request)
        )
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Current password is incorrect.");

        then(appUserRepository).should(never()).save(user);
    }

    @Test
    void updateUserStatusSuspendsAnotherUser() {
        AppUser user = existingUser();
        given(appUserRepository.findById(42L)).willReturn(Optional.of(user));
        given(appUserRepository.save(user)).willReturn(user);

        UserResponse response =
                appUserService.updateUserStatus(42L, false, "admin");

        assertThat(response.active()).isFalse();
        assertThat(user.isActive()).isFalse();
        then(appUserRepository).should().save(user);
    }

    @Test
    void updateUserStatusRejectsSelfSuspension() {
        AppUser user = existingUser();
        given(appUserRepository.findById(42L)).willReturn(Optional.of(user));

        assertThatThrownBy(() ->
                appUserService.updateUserStatus(42L, false, "CODER")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You cannot suspend your own account.");

        then(appUserRepository).should(never()).save(user);
    }

    @Test
    void updateUserStatusProtectsLastActiveAdministrator() {
        AppUser admin = existingUser();
        admin.setRole("ADMIN");
        given(appUserRepository.findById(42L)).willReturn(Optional.of(admin));
        given(appUserRepository.findAllByRoleIgnoreCaseAndActiveTrue("ADMIN"))
                .willReturn(List.of(admin));

        assertThatThrownBy(() ->
                appUserService.updateUserStatus(42L, false, "other-admin")
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("At least one active administrator is required.");

        then(appUserRepository).should(never()).save(admin);
    }

    @Test
    void updateUserRolePromotesUser() {
        AppUser user = existingUser();
        given(appUserRepository.findById(42L)).willReturn(Optional.of(user));
        given(appUserRepository.save(user)).willReturn(user);

        UserResponse response = appUserService.updateUserRole(42L, " admin ");

        assertThat(response.role()).isEqualTo("ADMIN");
        then(appUserRepository).should().save(user);
    }

    @Test
    void updateUserRoleDemotesAdministratorWhenAnotherRemains() {
        AppUser admin = existingUser();
        admin.setRole("ADMIN");
        AppUser otherAdmin = existingUser();
        otherAdmin.setUsername("other-admin");
        given(appUserRepository.findById(42L)).willReturn(Optional.of(admin));
        given(appUserRepository.findAllByRoleIgnoreCaseAndActiveTrue("ADMIN"))
                .willReturn(List.of(admin, otherAdmin));
        given(appUserRepository.save(admin)).willReturn(admin);

        UserResponse response = appUserService.updateUserRole(42L, "USER");

        assertThat(response.role()).isEqualTo("USER");
        then(appUserRepository).should().save(admin);
    }

    @Test
    void updateUserRoleProtectsLastActiveAdministrator() {
        AppUser admin = existingUser();
        admin.setRole("ADMIN");
        given(appUserRepository.findById(42L)).willReturn(Optional.of(admin));
        given(appUserRepository.findAllByRoleIgnoreCaseAndActiveTrue("ADMIN"))
                .willReturn(List.of(admin));

        assertThatThrownBy(() ->
                appUserService.updateUserRole(42L, "USER")
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("At least one active administrator is required.");

        then(appUserRepository).should(never()).save(admin);
    }

    private UserProfileUpdateRequest profileRequest(
            String username,
            String email
    ) {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        ReflectionTestUtils.setField(request, "username", username);
        ReflectionTestUtils.setField(request, "email", email);
        return request;
    }

    private PasswordChangeRequest passwordChangeRequest(
            String currentPassword,
            String newPassword
    ) {
        PasswordChangeRequest request = new PasswordChangeRequest();
        ReflectionTestUtils.setField(request, "currentPassword", currentPassword);
        ReflectionTestUtils.setField(request, "newPassword", newPassword);
        return request;
    }

    private AppUser existingUser() {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "createdAt", Instant.now());
        user.setUsername("coder");
        user.setEmail("coder@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole("USER");
        return user;
    }
}
