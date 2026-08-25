package InterCoach.dto;

public record CodeExecutionDockerSettingsResponse(
        String image,
        int cpuCount,
        int memoryMegabytes,
        int tmpfsMegabytes,
        int pidsLimit,
        boolean networkDisabled,
        boolean readOnlyRootFilesystem
) {
}
