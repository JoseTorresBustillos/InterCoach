package InterCoach.controller;

import InterCoach.dto.RecommendationResponse;
import InterCoach.security.UserAccessService;
import InterCoach.service.RecommendationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserAccessService userAccessService;

    public RecommendationController(
            RecommendationService recommendationService,
            UserAccessService userAccessService
    ) {
        this.recommendationService = recommendationService;
        this.userAccessService = userAccessService;
    }

    @GetMapping("/api/recommendations")
    public List<RecommendationResponse> getRecommendations() {
        return recommendationService.getRecommendations();
    }

    @GetMapping("/api/users/{userId}/recommendations")
    public List<RecommendationResponse> getRecommendationsForUser(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        userAccessService.assertCanAccessUser(userId, authentication);

        return recommendationService.getRecommendationsForUser(userId);
    }
}
