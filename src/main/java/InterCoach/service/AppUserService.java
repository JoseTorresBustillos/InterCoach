package InterCoach.service;

import InterCoach.dto.UserRequest;
import InterCoach.dto.UserResponse;
import InterCoach.exception.DuplicateResourceException;
import InterCoach.exception.ResourceNotFoundException;
import InterCoach.model.AppUser;
import InterCoach.repository.AppUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public UserResponse createUser(UserRequest request) {
        // Reject duplicates early so clients receive a clear failure reason.
        if (appUserRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new DuplicateResourceException("Username is already in use.");
        }

        if (appUserRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("Email is already in use.");
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());

        return toResponse(appUserRepository.save(user));
    }

    public List<UserResponse> getAllUsers() {
        return appUserRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        )
                );

        return toResponse(user);
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
