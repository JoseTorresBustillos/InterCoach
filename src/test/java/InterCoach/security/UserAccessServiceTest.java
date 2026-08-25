package InterCoach.security;

import InterCoach.exception.ResourceNotFoundException;
import InterCoach.model.AppUser;
import InterCoach.repository.AppUserRepository;
import InterCoach.repository.MockInterviewRepository;
import InterCoach.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserAccessServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private MockInterviewRepository mockInterviewRepository;

    private UserAccessService userAccessService;

    @BeforeEach
    void setUp() {
        userAccessService = new UserAccessService(
                appUserRepository,
                submissionRepository,
                mockInterviewRepository
        );
    }

    @Test
    void assertCanAccessUserAllowsOwner() {
        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user(42L, "coder", "USER")));

        assertThatCode(() ->
                userAccessService.assertCanAccessUser(
                        42L,
                        authentication("coder", "ROLE_USER")
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void assertCanAccessUserRejectsDifferentUser() {
        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user(42L, "coder", "USER")));

        assertThatThrownBy(() ->
                userAccessService.assertCanAccessUser(
                        99L,
                        authentication("coder", "ROLE_USER")
                )
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied.");
    }

    @Test
    void assertAdminAllowsAdminsWithoutUserLookup() {
        assertThatCode(() ->
                userAccessService.assertAdmin(
                        authentication("admin", "ROLE_ADMIN")
                )
        ).doesNotThrowAnyException();

        then(appUserRepository).shouldHaveNoInteractions();
    }

    @Test
    void assertCanAccessSubmissionAllowsOwner() {
        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user(42L, "coder", "USER")));
        given(submissionRepository.findUserIdById(10L))
                .willReturn(Optional.of(42L));

        assertThatCode(() ->
                userAccessService.assertCanAccessSubmission(
                        10L,
                        authentication("coder", "ROLE_USER")
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void assertCanAccessSubmissionRejectsDifferentUser() {
        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user(42L, "coder", "USER")));
        given(submissionRepository.findUserIdById(10L))
                .willReturn(Optional.of(99L));

        assertThatThrownBy(() ->
                userAccessService.assertCanAccessSubmission(
                        10L,
                        authentication("coder", "ROLE_USER")
                )
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied.");
    }

    @Test
    void assertCanAccessMockInterviewRejectsMissingSession() {
        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user(42L, "coder", "USER")));
        given(mockInterviewRepository.findUserIdById(77L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userAccessService.assertCanAccessMockInterview(
                        77L,
                        authentication("coder", "ROLE_USER")
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Mock interview not found with id: 77");
    }

    private Authentication authentication(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username,
                "password",
                AuthorityUtils.createAuthorityList(role)
        );
    }

    private AppUser user(Long id, String username, String role) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setRole(role);
        return user;
    }
}
