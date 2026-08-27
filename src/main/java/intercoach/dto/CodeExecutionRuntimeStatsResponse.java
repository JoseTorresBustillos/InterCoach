package intercoach.dto;

import java.time.Instant;

public record CodeExecutionRuntimeStatsResponse(
        long totalRuns,
        long successfulRuns,
        long failedRuns,
        long compileErrorRuns,
        long runtimeErrorRuns,
        long timeoutRuns,
        long wrongAnswerRuns,
        long unsupportedLanguageRuns,
        long sourceTooLargeRuns,
        long noTestRuns,
        long averageDurationMs,
        long lastDurationMs,
        String lastStatus,
        Instant lastRunAt
) {
}
