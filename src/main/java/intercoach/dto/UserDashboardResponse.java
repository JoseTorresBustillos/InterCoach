package intercoach.dto;

import java.time.Instant;
import java.util.List;

public record UserDashboardResponse(
        Long userId,
        String username,
        String email,
        Instant memberSince,
        long totalSubmissions,
        long reviewedSubmissions,
        long failedSubmissions,
        long pendingSubmissions,
        long attemptedProblems,
        long reviewedProblems,
        long mockInterviewsStarted,
        long mockInterviewsCompleted,
        long mockInterviewsInProgress,
        long mockInterviewsAbandoned,
        List<ProgressBucketResponse> submissionsByDifficulty,
        List<ProgressBucketResponse> submissionsByCategory,
        List<RecentSubmissionSummaryResponse> recentSubmissions,
        List<RecentMockInterviewSummaryResponse> recentMockInterviews
) {
}
