package InterCoach.service;

import InterCoach.config.CodeExecutionProperties;
import InterCoach.dto.CodeExecutionOperationsResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeExecutionOperationsServiceTest {

    @Test
    void getOperationsStatusReportsConfiguredLimitsAndSafeguards() {
        CodeExecutionProperties properties = new CodeExecutionProperties();
        properties.setMode(CodeExecutionProperties.ExecutionMode.DOCKER);
        properties.setCompileTimeoutSeconds(7);
        properties.setTestTimeoutSeconds(3);
        properties.setOutputLimitCharacters(2048);
        properties.setMaxSourceCharacters(12000);
        properties.setMaxHeapMegabytes(96);
        properties.setActiveProcessorCount(2);
        properties.setDockerImage("example/java-runner:21");
        properties.setDockerCpuCount(2);
        properties.setDockerMemoryMegabytes(384);
        properties.setDockerTmpfsMegabytes(48);
        properties.setDockerPidsLimit(32);
        CodeExecutionOperationsService service =
                new CodeExecutionOperationsService(properties);

        CodeExecutionOperationsResponse response =
                service.getOperationsStatus();

        assertThat(response.mode()).isEqualTo("DOCKER");
        assertThat(response.supportedLanguage()).isEqualTo("Java");
        assertThat(response.compileTimeoutSeconds()).isEqualTo(7);
        assertThat(response.testTimeoutSeconds()).isEqualTo(3);
        assertThat(response.outputLimitCharacters()).isEqualTo(2048);
        assertThat(response.maxSourceCharacters()).isEqualTo(12000);
        assertThat(response.maxHeapMegabytes()).isEqualTo(96);
        assertThat(response.activeProcessorCount()).isEqualTo(2);
        assertThat(response.visibleTestCasesOnly()).isTrue();
        assertThat(response.temporaryWorkspacePerRun()).isTrue();
        assertThat(response.childEnvironmentSanitized()).isTrue();
        assertThat(response.docker().image()).isEqualTo("example/java-runner:21");
        assertThat(response.docker().cpuCount()).isEqualTo(2);
        assertThat(response.docker().memoryMegabytes()).isEqualTo(384);
        assertThat(response.docker().tmpfsMegabytes()).isEqualTo(48);
        assertThat(response.docker().pidsLimit()).isEqualTo(32);
        assertThat(response.docker().networkDisabled()).isTrue();
        assertThat(response.docker().readOnlyRootFilesystem()).isTrue();
    }
}
