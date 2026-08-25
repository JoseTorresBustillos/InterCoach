package intercoach.security;

import intercoach.exception.ResourceNotFoundException;
import intercoach.model.AppUser;
import intercoach.repository.AppUserRepository;
import intercoach.repository.MockInterviewRepository;
import intercoach.repository.SubmissionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UserAccessService {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";
    private static final String ACCESS_DENIED = "Access denied.";

    private final AppUserRepository appUserRepository;
    private final SubmissionRepository submissionRepository;
    private final MockInterviewRepository mockInterviewRepository;

    public UserAccessService(
            AppUserRepository appUserRepository,
            SubmissionRepository submissionRepository,
            MockInterviewRepository mockInterviewRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.submissionRepository = submissionRepository;
        this.mockInterviewRepository = mockInterviewRepository;
    }

    public void assertAdmin(Authentication authentication) {
        if (!isAdmin(authentication)) {
            throwDenied();
        }
    }

    public void assertCanAccessUser(
            Long userId,
            Authentication authentication
    ) {
        if (isAdmin(authentication)) {
            return;
        }

        Long currentUserId = currentUserId(authentication);

        if (!Objects.equals(currentUserId, userId)) {
            throwDenied();
        }
    }

    public void assertCanAccessSubmission(
            Long submissionId,
            Authentication authentication
    ) {
        if (isAdmin(authentication)) {
            return;
        }

        Long currentUserId = currentUserId(authentication);
        Long ownerId = submissionRepository.findUserIdById(submissionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Submission not found with id: "
                                        + submissionId
                        )
                );

        if (!Objects.equals(currentUserId, ownerId)) {
            throwDenied();
        }
    }

    public void assertCanAccessMockInterview(
            Long sessionId,
            Authentication authentication
    ) {
        if (isAdmin(authentication)) {
            return;
        }

        Long currentUserId = currentUserId(authentication);
        Long ownerId = mockInterviewRepository.findUserIdById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Mock interview not found with id: "
                                        + sessionId
                        )
                );

        if (!Objects.equals(currentUserId, ownerId)) {
            throwDenied();
        }
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throwDenied();
        }

        AppUser user = appUserRepository
                .findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(this::accessDenied);

        return user.getId();
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        ADMIN_AUTHORITY.equals(authority.getAuthority())
                );
    }

    private void throwDenied() {
        throw accessDenied();
    }

    private AccessDeniedException accessDenied() {
        return new AccessDeniedException(ACCESS_DENIED);
    }
}
