package InterCoach.service;

import InterCoach.dto.AuthResponse;
import InterCoach.dto.LoginRequest;
import InterCoach.dto.RegisterRequest;
import InterCoach.dto.UserResponse;
import InterCoach.exception.AuthenticationFailedException;
import InterCoach.exception.DuplicateResourceException;
import InterCoach.model.AppUser;
import InterCoach.repository.AppUserRepository;
import InterCoach.security.JwtService;
import InterCoach.security.JwtToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        // Enforce uniqueness before hashing so rejected requests do less work.
        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("Username is already in use.");
        }

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already in use.");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        AppUser savedUser = appUserRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsernameOrEmail().trim();

        AppUser user = appUserRepository.findByUsernameIgnoreCase(identifier)
                .or(() -> appUserRepository.findByEmailIgnoreCase(identifier))
                .orElseThrow(() -> new AuthenticationFailedException(
                        "Invalid username/email or password."
                ));

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash()
                )) {
            throw new AuthenticationFailedException(
                    "Invalid username/email or password."
            );
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(AppUser user) {
        JwtToken token = jwtService.generateToken(user);

        return new AuthResponse(
                token.value(),
                "Bearer",
                token.expiresAt(),
                new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getCreatedAt()
                )
        );
    }
}
