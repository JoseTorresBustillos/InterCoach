package intercoach.progress.controller;

import intercoach.auth.security.UserAccessService;
import intercoach.progress.dto.UserAnalyticsResponse;
import intercoach.progress.service.UserAnalyticsService;
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
