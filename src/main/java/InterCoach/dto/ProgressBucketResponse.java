package InterCoach.dto;

public record ProgressBucketResponse(
        String name,
        long totalSubmissions,
        long reviewedSubmissions,
        long failedSubmissions,
        long pendingSubmissions
) {
}
