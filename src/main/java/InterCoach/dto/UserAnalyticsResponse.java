package InterCoach.dto;

import java.util.List;

public record UserAnalyticsResponse(
        Long userId,
        String username,
        long totalSubmissions,
        long reviewedSubmissions,
        long failedSubmissions,
        long pendingSubmissions,
        long weakSubmissions,
        long strongReviewedSubmissions,
        long distinctProblemsAttempted,
        long distinctProblemsReviewed,
        long totalMockInterviews,
        long completedMockInterviews,
        double reviewRate,
        double weakSubmissionRate,
        String strongestCategory,
        String weakestCategory,
        List<UserAnalyticsBucketResponse> difficultyBreakdown,
        List<UserAnalyticsBucketResponse> categoryBreakdown,
        List<UserActivityTrendResponse> activityTrend
) {
}
