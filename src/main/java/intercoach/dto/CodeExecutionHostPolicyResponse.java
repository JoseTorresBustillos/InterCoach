package intercoach.dto;

public record CodeExecutionHostPolicyResponse(
        String isolation,
        boolean localExecutionEnabled,
        boolean osLevelIsolation,
        boolean networkDisabled,
        boolean readOnlyRootFilesystem,
        String workspacePolicy
) {
}
