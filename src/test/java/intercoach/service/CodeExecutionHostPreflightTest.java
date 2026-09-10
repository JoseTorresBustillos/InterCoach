package intercoach.service;

import intercoach.config.CodeExecutionProperties;
import intercoach.dto.CodeExecutionPreflightResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeExecutionHostPreflightTest {

    @Test
    void localModeIsReadyWhenIsolationIsOptional() {
        CodeExecutionProperties properties = new CodeExecutionProperties();
        List<List<String>> commands = new ArrayList<>();
        CodeExecutionHostPreflight preflight = new CodeExecutionHostPreflight(
                properties,
                (command, timeoutSeconds) -> {
                    commands.add(command);
                    return true;
                }
        );

        preflight.checkHost();

        CodeExecutionPreflightResponse response = preflight.snapshot();
        assertThat(response.checked()).isTrue();
        assertThat(response.hostReady()).isTrue();
        assertThat(response.dockerAvailable()).isFalse();
        assertThat(response.imageReady()).isFalse();
        assertThat(response.checkedAt()).isNotNull();
        assertThat(commands).isEmpty();
    }

    @Test
    void localModeFailsWhenIsolationIsRequired() {
        CodeExecutionProperties properties = new CodeExecutionProperties();
        properties.setRequireOsIsolation(true);
        CodeExecutionHostPreflight preflight =
                new CodeExecutionHostPreflight(properties);

        assertThatThrownBy(preflight::checkHost)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OS-level isolation is required");

        assertThat(preflight.snapshot().checked()).isTrue();
        assertThat(preflight.snapshot().hostReady()).isFalse();
    }

    @Test
    void dockerModeAcceptsCachedRunnerImage() {
        CodeExecutionProperties properties = dockerProperties();
        List<List<String>> commands = new ArrayList<>();
        CodeExecutionHostPreflight preflight = new CodeExecutionHostPreflight(
                properties,
                (command, timeoutSeconds) -> {
                    commands.add(command);
                    assertThat(timeoutSeconds).isEqualTo(45);
                    return true;
                }
        );

        preflight.checkHost();

        assertThat(commands).containsExactly(
                List.of("docker", "info"),
                List.of("docker", "image", "inspect", "example/runner:21")
        );
        assertThat(preflight.snapshot().hostReady()).isTrue();
        assertThat(preflight.snapshot().dockerAvailable()).isTrue();
        assertThat(preflight.snapshot().imageReady()).isTrue();
    }

    @Test
    void dockerModePrePullsMissingRunnerImage() {
        CodeExecutionProperties properties = dockerProperties();
        List<List<String>> commands = new ArrayList<>();
        CodeExecutionHostPreflight preflight = new CodeExecutionHostPreflight(
                properties,
                (command, timeoutSeconds) -> {
                    commands.add(command);
                    return !command.contains("inspect");
                }
        );

        preflight.checkHost();

        assertThat(commands).containsExactly(
                List.of("docker", "info"),
                List.of("docker", "image", "inspect", "example/runner:21"),
                List.of("docker", "pull", "example/runner:21")
        );
        assertThat(preflight.snapshot().imageReady()).isTrue();
        assertThat(preflight.snapshot().message()).contains("pre-pulled");
    }

    @Test
    void dockerModeFailsWhenMissingImageCannotBePulled() {
        CodeExecutionProperties properties = dockerProperties();
        CodeExecutionHostPreflight preflight = new CodeExecutionHostPreflight(
                properties,
                (command, timeoutSeconds) -> command.equals(
                        List.of("docker", "info")
                )
        );

        assertThatThrownBy(preflight::checkHost)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be prepared");

        assertThat(preflight.snapshot().dockerAvailable()).isTrue();
        assertThat(preflight.snapshot().imageReady()).isFalse();
    }

    @Test
    void dockerModeFailsForMissingImageWhenPrePullIsDisabled() {
        CodeExecutionProperties properties = dockerProperties();
        properties.setDockerPrePull(false);
        List<List<String>> commands = new ArrayList<>();
        CodeExecutionHostPreflight preflight = new CodeExecutionHostPreflight(
                properties,
                (command, timeoutSeconds) -> {
                    commands.add(command);
                    return command.equals(List.of("docker", "info"));
                }
        );

        assertThatThrownBy(preflight::checkHost)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pre-pull is disabled");

        assertThat(commands).containsExactly(
                List.of("docker", "info"),
                List.of("docker", "image", "inspect", "example/runner:21")
        );
        assertThat(preflight.snapshot().imagePrePullEnabled()).isFalse();
    }

    @Test
    void dockerModeFailsWhenHostIsUnavailable() {
        CodeExecutionProperties properties = dockerProperties();
        CodeExecutionHostPreflight preflight = new CodeExecutionHostPreflight(
                properties,
                (command, timeoutSeconds) -> false
        );

        assertThatThrownBy(preflight::checkHost)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Docker host is unavailable");

        assertThat(preflight.snapshot().dockerAvailable()).isFalse();
    }

    private CodeExecutionProperties dockerProperties() {
        CodeExecutionProperties properties = new CodeExecutionProperties();
        properties.setMode(CodeExecutionProperties.ExecutionMode.DOCKER);
        properties.setDockerImage("example/runner:21");
        properties.setDockerPreflightTimeoutSeconds(45);
        return properties;
    }
}
