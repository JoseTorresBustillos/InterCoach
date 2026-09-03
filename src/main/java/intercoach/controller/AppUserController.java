package intercoach.controller;

import intercoach.dto.AuthResponse;
import intercoach.dto.PasswordChangeRequest;
import intercoach.dto.UserProfileUpdateRequest;
import intercoach.dto.UserRequest;
import intercoach.dto.UserResponse;
import intercoach.dto.UserRoleUpdateRequest;
import intercoach.dto.UserStatusUpdateRequest;
import intercoach.security.UserAccessService;
import intercoach.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    @PatchMapping("/{userId}/status")
    public UserResponse updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request,
            Authentication authentication
    ) {
        userAccessService.assertAdmin(authentication);

        return appUserService.updateUserStatus(
                userId,
                request.active(),
                authentication.getName()
        );
    }

    @PatchMapping("/{userId}/role")
    public UserResponse updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequest request,
            Authentication authentication
    ) {
        userAccessService.assertAdmin(authentication);
        return appUserService.updateUserRole(userId, request.role());
    }
}
