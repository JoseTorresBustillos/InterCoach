package InterCoach.controller;

import InterCoach.dto.AuthResponse;
import InterCoach.dto.PasswordChangeRequest;
import InterCoach.dto.UserProfileUpdateRequest;
import InterCoach.dto.UserRequest;
import InterCoach.dto.UserResponse;
import InterCoach.security.UserAccessService;
import InterCoach.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class AppUserController {

    private final AppUserService appUserService;
    private final UserAccessService userAccessService;

    public AppUserController(
            AppUserService appUserService,
            UserAccessService userAccessService
    ) {
        this.appUserService = appUserService;
        this.userAccessService = userAccessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody UserRequest request,
            Authentication authentication
    ) {
        userAccessService.assertAdmin(authentication);

        return appUserService.createUser(request);
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication) {
        return appUserService.getUserByUsername(authentication.getName());
    }

    @PatchMapping("/me")
    public AuthResponse updateCurrentUserProfile(
            @Valid @RequestBody UserProfileUpdateRequest request,
            Authentication authentication
    ) {
        return appUserService.updateCurrentUserProfile(
                authentication.getName(),
                request
        );
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeCurrentUserPassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Authentication authentication
    ) {
        appUserService.changeCurrentUserPassword(
                authentication.getName(),
                request
        );
    }

    @GetMapping
    public List<UserResponse> getAllUsers(Authentication authentication) {
        userAccessService.assertAdmin(authentication);

        return appUserService.getAllUsers();
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        userAccessService.assertCanAccessUser(userId, authentication);

        return appUserService.getUserById(userId);
    }
}
