package intercoach.dto;

public record CodeExecutionOperationsResponse(
        String mode,
        String supportedLanguage,
        int compileTimeoutSeconds,
        int testTimeoutSeconds,
        int outputLimitCharacters,
        int maxSourceCharacters,
        int maxHeapMegabytes,
        int activeProcessorCount,
        int maxConcurrentRuns,
        boolean visibleTestCasesOnly,
        boolean temporaryWorkspacePerRun,
        boolean childEnvironmentSanitized,
        CodeExecutionHostPolicyResponse hostPolicy,
        CodeExecutionPreflightResponse preflight,
        CodeExecutionRuntimeStatsResponse runtime,
        CodeExecutionDockerSettingsResponse docker
) {
}
