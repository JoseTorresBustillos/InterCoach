package intercoach.service;

import intercoach.dto.AuthResponse;
import intercoach.dto.PasswordChangeRequest;
import intercoach.dto.UserProfileUpdateRequest;
import intercoach.dto.UserRequest;
import intercoach.dto.UserResponse;
import intercoach.exception.AuthenticationFailedException;
import intercoach.exception.DuplicateResourceException;
import intercoach.exception.ResourceNotFoundException;
import intercoach.model.AppUser;
import intercoach.repository.AppUserRepository;
import intercoach.security.JwtService;
import intercoach.security.JwtToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserService {

    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 50;
    private static final int EMAIL_MAX_LENGTH = 255;

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AppUserService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse createUser(UserRequest request) {
        String username = requiredUsername(request.getUsername());
        String email = requiredEmail(request.getEmail());

        // Reject duplicates early so clients receive a clear failure reason.
        ensureUsernameAvailable(username);
        ensureEmailAvailable(email);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        return toResponse(appUserRepository.save(user));
    }

    public List<UserResponse> getAllUsers() {
        return appUserRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long userId) {
        return toResponse(findById(userId));
    }

    public UserResponse getUserByUsername(String username) {
        AppUser user = appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username: " + username
                        )
                );

        return toResponse(user);
    }

    public UserResponse updateUserStatus(
            Long userId,
            boolean active,
            String actingUsername
    ) {
        AppUser user = findById(userId);

        if (!active && user.getUsername().equalsIgnoreCase(actingUsername)) {
            throw new IllegalArgumentException(
                    "You cannot suspend your own account."
            );
        }

        if (user.isActive() == active) {
            return toResponse(user);
        }

        user.setActive(active);
        return toResponse(appUserRepository.save(user));
    }

    public AuthResponse updateCurrentUserProfile(
            String currentUsername,
            UserProfileUpdateRequest request
    ) {
        AppUser user = findByUsername(currentUsername);
        String username = optionalUsername(request.getUsername());
        String email = optionalEmail(request.getEmail());

        if (username == null && email == null) {
            throw new IllegalArgumentException(
                    "At least one profile field must be provided."
            );
        }

        if (username != null
                && !username.equalsIgnoreCase(user.getUsername())) {
            ensureUsernameAvailable(username);
            user.setUsername(username);
        }

        if (email != null && !email.equalsIgnoreCase(user.getEmail())) {
            ensureEmailAvailable(email);
            user.setEmail(email);
        }

        return buildAuthResponse(appUserRepository.save(user));
    }

    public void changeCurrentUserPassword(
            String currentUsername,
            PasswordChangeRequest request
    ) {
        AppUser user = findByUsername(currentUsername);

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(
                        request.getCurrentPassword(),
                        user.getPasswordHash()
                )) {
            throw new AuthenticationFailedException(
                    "Current password is incorrect."
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        appUserRepository.save(user);
    }

    private AppUser findByUsername(String username) {
        return appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username: " + username
                        )
                );
    }

    private AppUser findById(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        )
                );
    }

    private void ensureUsernameAvailable(String username) {
        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("Username is already in use.");
        }
    }

    private void ensureEmailAvailable(String email) {
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already in use.");
        }
    }

    private String requiredUsername(String value) {
        String username = requiredTrimmed(value, "Username");
        validateUsername(username);
        return username;
    }

    private String optionalUsername(String value) {
        String username = optionalTrimmed(value);

        if (username != null) {
            validateUsername(username);
        }

        return username;
    }

    private String requiredEmail(String value) {
        return normalizeEmail(requiredTrimmed(value, "Email"));
    }

    private String optionalEmail(String value) {
        String email = optionalTrimmed(value);

        if (email == null) {
            return null;
        }

        return normalizeEmail(email);
    }

    private String requiredTrimmed(String value, String fieldName) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }

        return value.trim();
    }

    private String optionalTrimmed(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(
                    "Profile fields must not be blank."
            );
        }

        return trimmed;
    }

    private void validateUsername(String username) {
        if (username.length() < USERNAME_MIN_LENGTH
                || username.length() > USERNAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Username must be between 3 and 50 characters."
            );
        }
    }

    private String normalizeEmail(String email) {
        String normalized = email.toLowerCase();

        if (normalized.length() > EMAIL_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Email must be at most 255 characters."
            );
        }

        return normalized;
    }

    private AuthResponse buildAuthResponse(AppUser user) {
        JwtToken token = jwtService.generateToken(user);

        return new AuthResponse(
                token.value(),
                "Bearer",
                token.expiresAt(),
                toResponse(user)
        );
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
