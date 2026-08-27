package intercoach.service;

import intercoach.config.CodeExecutionProperties;
import intercoach.dto.CodeExecutionResponse;
import intercoach.dto.CodeExecutionOperationsResponse;
import intercoach.dto.CodeExecutionStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        CodeExecutionRunMonitor runMonitor = new CodeExecutionRunMonitor();
        CodeExecutionOperationsService service =
                new CodeExecutionOperationsService(properties, runMonitor);

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
        assertThat(response.hostPolicy().isolation()).isEqualTo("Docker container");
        assertThat(response.hostPolicy().localExecutionEnabled()).isFalse();
        assertThat(response.hostPolicy().osLevelIsolation()).isTrue();
        assertThat(response.hostPolicy().networkDisabled()).isTrue();
        assertThat(response.hostPolicy().readOnlyRootFilesystem()).isTrue();
        assertThat(response.runtime().totalRuns()).isZero();
        assertThat(response.runtime().failedRuns()).isZero();
        assertThat(response.runtime().lastStatus()).isNull();
        assertThat(response.docker().image()).isEqualTo("example/java-runner:21");
        assertThat(response.docker().cpuCount()).isEqualTo(2);
        assertThat(response.docker().memoryMegabytes()).isEqualTo(384);
        assertThat(response.docker().tmpfsMegabytes()).isEqualTo(48);
        assertThat(response.docker().pidsLimit()).isEqualTo(32);
        assertThat(response.docker().networkDisabled()).isTrue();
        assertThat(response.docker().readOnlyRootFilesystem()).isTrue();
    }

    @Test
    void getOperationsStatusReportsExecutionRuntimeStatistics() {
        CodeExecutionProperties properties = new CodeExecutionProperties();
        CodeExecutionRunMonitor runMonitor = new CodeExecutionRunMonitor();
        CodeExecutionOperationsService service =
                new CodeExecutionOperationsService(properties, runMonitor);

        runMonitor.record(response(CodeExecutionStatus.SUCCESS, 40));
        runMonitor.record(response(CodeExecutionStatus.TIME_LIMIT_EXCEEDED, 20));
        runMonitor.record(response(CodeExecutionStatus.COMPILE_ERROR, 30));

        CodeExecutionOperationsResponse response =
                service.getOperationsStatus();

        assertThat(response.hostPolicy().isolation())
                .isEqualTo("Local child process");
        assertThat(response.hostPolicy().localExecutionEnabled()).isTrue();
        assertThat(response.hostPolicy().osLevelIsolation()).isFalse();
        assertThat(response.runtime().totalRuns()).isEqualTo(3);
        assertThat(response.runtime().successfulRuns()).isEqualTo(1);
        assertThat(response.runtime().failedRuns()).isEqualTo(2);
        assertThat(response.runtime().compileErrorRuns()).isEqualTo(1);
        assertThat(response.runtime().timeoutRuns()).isEqualTo(1);
        assertThat(response.runtime().averageDurationMs()).isEqualTo(30);
        assertThat(response.runtime().lastDurationMs()).isEqualTo(30);
        assertThat(response.runtime().lastStatus()).isEqualTo("COMPILE_ERROR");
        assertThat(response.runtime().lastRunAt()).isNotNull();
    }

    private CodeExecutionResponse response(
            CodeExecutionStatus status,
            long durationMs
    ) {
        return new CodeExecutionResponse(
                1L,
                status,
                status == CodeExecutionStatus.SUCCESS,
                status == CodeExecutionStatus.SUCCESS ? 1 : 0,
                1,
                durationMs,
                "",
                List.of()
        );
    }
}
