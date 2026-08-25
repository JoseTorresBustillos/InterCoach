package InterCoach.controller;

import InterCoach.dto.UserAnalyticsResponse;
import InterCoach.security.UserAccessService;
import InterCoach.service.UserAnalyticsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserAnalyticsController {

    private final UserAnalyticsService userAnalyticsService;
    private final UserAccessService userAccessService;

    public UserAnalyticsController(
            UserAnalyticsService userAnalyticsService,
            UserAccessService userAccessService
    ) {
        this.userAnalyticsService = userAnalyticsService;
        this.userAccessService = userAccessService;
    }

    @GetMapping("/api/users/{userId}/analytics")
    public UserAnalyticsResponse getAnalytics(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        userAccessService.assertCanAccessUser(userId, authentication);

        return userAnalyticsService.getAnalytics(userId);
    }
}
