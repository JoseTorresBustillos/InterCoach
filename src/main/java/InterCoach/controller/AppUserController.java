package InterCoach.controller;

import InterCoach.dto.UserRequest;
import InterCoach.dto.UserResponse;
import InterCoach.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        return appUserService.createUser(request);
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return appUserService.getAllUsers();
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable Long userId) {
        return appUserService.getUserById(userId);
    }
}