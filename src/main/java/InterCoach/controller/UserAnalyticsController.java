package InterCoach.controller;

import InterCoach.dto.UserAnalyticsResponse;
import InterCoach.service.UserAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserAnalyticsController {

    private final UserAnalyticsService userAnalyticsService;

    public UserAnalyticsController(UserAnalyticsService userAnalyticsService) {
        this.userAnalyticsService = userAnalyticsService;
    }

    @GetMapping("/api/users/{userId}/analytics")
    public UserAnalyticsResponse getAnalytics(@PathVariable Long userId) {
        return userAnalyticsService.getAnalytics(userId);
    }
}
