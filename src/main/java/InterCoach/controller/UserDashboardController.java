package InterCoach.controller;

import InterCoach.dto.UserDashboardResponse;
import InterCoach.service.UserDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserDashboardController {

    private final UserDashboardService userDashboardService;

    public UserDashboardController(
            UserDashboardService userDashboardService
    ) {
        this.userDashboardService = userDashboardService;
    }

    @GetMapping("/api/users/{userId}/dashboard")
    public UserDashboardResponse getDashboard(@PathVariable Long userId) {
        return userDashboardService.getDashboard(userId);
    }
}
