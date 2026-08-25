package intercoach.dto;

import java.time.Instant;

public record UserAnalyticsBucketResponse(
        String name,
        long totalSubmissions,
        long reviewedSubmissions,
        long failedSubmissions,
        long weakSubmissions,
        double weakSubmissionRate,
        Instant latestActivityAt
) {
}
