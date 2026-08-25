package InterCoach.service;

import InterCoach.config.CodeExecutionProperties;
import InterCoach.dto.CodeExecutionDockerSettingsResponse;
import InterCoach.dto.CodeExecutionOperationsResponse;
import org.springframework.stereotype.Service;

@Service
public class CodeExecutionOperationsService {

    private static final String SUPPORTED_LANGUAGE = "Java";

    private final CodeExecutionProperties properties;

    public CodeExecutionOperationsService(CodeExecutionProperties properties) {
        this.properties = properties;
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
                dockerSettings()
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
