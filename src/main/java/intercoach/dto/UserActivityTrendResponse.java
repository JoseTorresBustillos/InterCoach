package intercoach.dto;

import java.time.LocalDate;

public record UserActivityTrendResponse(
        LocalDate date,
        long submissions,
        long reviewedSubmissions,
        long failedSubmissions,
        long mockInterviews,
        long completedMockInterviews
) {
}
