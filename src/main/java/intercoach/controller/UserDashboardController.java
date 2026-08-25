package intercoach.controller;

import intercoach.dto.UserDashboardResponse;
import intercoach.security.UserAccessService;
import intercoach.service.UserDashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserDashboardController {

    private final UserDashboardService userDashboardService;
    private final UserAccessService userAccessService;

    public UserDashboardController(
            UserDashboardService userDashboardService,
            UserAccessService userAccessService
    ) {
        this.userDashboardService = userDashboardService;
        this.userAccessService = userAccessService;
    }

    @GetMapping("/api/users/{userId}/dashboard")
    public UserDashboardResponse getDashboard(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        userAccessService.assertCanAccessUser(userId, authentication);

        return userDashboardService.getDashboard(userId);
    }
}
