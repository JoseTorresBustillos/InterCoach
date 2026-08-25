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
        boolean visibleTestCasesOnly,
        boolean temporaryWorkspacePerRun,
        boolean childEnvironmentSanitized,
        CodeExecutionDockerSettingsResponse docker
) {
}
