package intercoach.recommendation.controller;

import intercoach.auth.security.UserAccessService;
import intercoach.recommendation.dto.RecommendationResponse;
import intercoach.recommendation.service.RecommendationService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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
