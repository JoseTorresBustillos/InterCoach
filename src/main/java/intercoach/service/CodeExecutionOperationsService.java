package intercoach.service;

import intercoach.config.CodeExecutionProperties;
import intercoach.dto.CodeExecutionDockerSettingsResponse;
import intercoach.dto.CodeExecutionHostPolicyResponse;
import intercoach.dto.CodeExecutionOperationsResponse;
import org.springframework.stereotype.Service;

@Service
public class CodeExecutionOperationsService {

    private static final String SUPPORTED_LANGUAGE = "Java";

    private final CodeExecutionProperties properties;
    private final CodeExecutionRunMonitor runMonitor;

    public CodeExecutionOperationsService(
            CodeExecutionProperties properties,
            CodeExecutionRunMonitor runMonitor
    ) {
        this.properties = properties;
        this.runMonitor = runMonitor;
    }

    public CodeExecutionOperationsResponse getOperationsStatus() {
        return new CodeExecutionOperationsResponse(
                properties.getMode().name(),
                SUPPORTED_LANGUAGE,
                properties.getCompileTimeoutSeconds(),
                properties.getTestTimeoutSeconds(),
                properties.getOutputLimitCharacters(),
                properties.getMaxSourceCharacters(),
                properties.getMaxHeapMegabytes(),
                properties.getActiveProcessorCount(),
                true,
                true,
                true,
                hostPolicy(),
                runMonitor.snapshot(),
                dockerSettings()
        );
    }

    private CodeExecutionHostPolicyResponse hostPolicy() {
        boolean dockerMode = properties.getMode()
                == CodeExecutionProperties.ExecutionMode.DOCKER;

        return new CodeExecutionHostPolicyResponse(
                dockerMode ? "Docker container" : "Local child process",
                !dockerMode,
                dockerMode,
                dockerMode,
                dockerMode,
                "Temporary workspace per run"
        );
    }

    private CodeExecutionDockerSettingsResponse dockerSettings() {
        return new CodeExecutionDockerSettingsResponse(
                properties.getDockerImage(),
                properties.getDockerCpuCount(),
                properties.getDockerMemoryMegabytes(),
                properties.getDockerTmpfsMegabytes(),
                properties.getDockerPidsLimit(),
                true,
                true
        );
    }
}
