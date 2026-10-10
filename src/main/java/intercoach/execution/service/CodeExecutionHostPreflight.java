package intercoach.execution.service;

import intercoach.execution.config.CodeExecutionProperties;
import intercoach.execution.dto.CodeExecutionPreflightResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Service
public class CodeExecutionHostPreflight implements ApplicationRunner {

    private static final String DOCKER_COMMAND = "docker";
    private static final Logger LOGGER = LoggerFactory.getLogger(CodeExecutionHostPreflight.class);

    private final CodeExecutionProperties properties;
    private final CommandExecutor commandExecutor;
    private final AtomicReference<CodeExecutionPreflightResponse> status;

    @Autowired
    public CodeExecutionHostPreflight(CodeExecutionProperties properties) {
        this(properties, new ProcessCommandExecutor());
    }

    CodeExecutionHostPreflight(
            CodeExecutionProperties properties,
            CommandExecutor commandExecutor
    ) {
        this.properties = properties;
        this.commandExecutor = commandExecutor;
        this.status = new AtomicReference<>(notChecked());
    }

    @Override
    public void run(ApplicationArguments arguments) {
        checkHost();
    }

    public CodeExecutionPreflightResponse snapshot() {
        return status.get();
    }

    void checkHost() {
        if (!dockerMode()) {
            checkLocalMode();
            return;
        }

        int timeoutSeconds = properties.getDockerPreflightTimeoutSeconds();

        if (!run(List.of(DOCKER_COMMAND, "info"), timeoutSeconds)) {
            fail(
                    "Docker execution is configured, but the Docker host "
                            + "is unavailable.",
                    false,
                    false
            );
        }

        List<String> inspectCommand = List.of(
                DOCKER_COMMAND,
                "image",
                "inspect",
                properties.getDockerImage()
        );

        if (run(inspectCommand, timeoutSeconds)) {
            checkWorkspace(timeoutSeconds);
            ready(true, true, "Docker host, runner image, and shared workspace are ready.");
            return;
        }

        if (!properties.isDockerPrePull()) {
            fail(
                    "Docker runner image is not available locally and image "
                            + "pre-pull is disabled.",
                    true,
                    false
            );
        }

        if (!run(
                List.of(
                        DOCKER_COMMAND,
                        "pull",
                        properties.getDockerImage()
                ),
                timeoutSeconds
        )) {
            fail(
                    "Docker runner image could not be prepared within the "
                            + "configured preflight timeout.",
                    true,
                    false
            );
        }

        checkWorkspace(timeoutSeconds);
        ready(true, true, "Docker runner image was pre-pulled; shared workspace is ready.");
    }

    private void checkWorkspace(int timeoutSeconds) {
        Path workspace = null;
        try {
            Files.createDirectories(properties.getWorkspaceRoot());
            workspace = Files.createTempDirectory(properties.getWorkspaceRoot(), "intercoach-preflight-");
            String token = UUID.randomUUID().toString();
            Files.writeString(workspace.resolve("probe"), token);
            // A remote daemon must see the application's actual files at the same absolute path.
            boolean visible = run(List.of(
                    DOCKER_COMMAND, "run", "--rm", "--name", "intercoach-preflight-" + UUID.randomUUID(),
                    "--network", "none", "--read-only", "--cpus", "1", "--memory", "64m",
                    "--pids-limit", "16", "--mount",
                    "type=bind,source=" + workspace.toAbsolutePath() + ",target=/workspace,readonly",
                    "--entrypoint", "sh", properties.getDockerImage(), "-c",
                    "test \"$(cat /workspace/probe)\" = \"$1\"", "sh", token
            ), timeoutSeconds);
            if (!visible) {
                fail("Docker runner cannot read the shared execution workspace.", true, true);
            }
        } catch (IOException exception) {
            fail("Execution workspace cannot be prepared.", true, true);
        } finally {
            if (workspace != null) {
                try {
                    Files.deleteIfExists(workspace.resolve("probe"));
                    Files.deleteIfExists(workspace);
                } catch (IOException exception) {
                    LOGGER.warn("Could not remove execution preflight workspace", exception);
                }
            }
        }
    }

    private void checkLocalMode() {
        if (properties.isRequireOsIsolation()) {
            fail(
                    "OS-level isolation is required, but local code execution "
                            + "is configured.",
                    false,
                    false
            );
        }

        ready(
                false,
                false,
                "Local execution is allowed by the configured host policy."
        );
    }

    private boolean run(List<String> command, int timeoutSeconds) {
        return commandExecutor.run(command, timeoutSeconds);
    }

    private void ready(
            boolean dockerAvailable,
            boolean imageReady,
            String message
    ) {
        status.set(response(
                true,
                true,
                dockerAvailable,
                imageReady,
                message,
                Instant.now()
        ));
    }

    private void fail(
            String message,
            boolean dockerAvailable,
            boolean imageReady
    ) {
        status.set(response(
                true,
                false,
                dockerAvailable,
                imageReady,
                message,
                Instant.now()
        ));
        throw new IllegalStateException(message);
    }

    private CodeExecutionPreflightResponse notChecked() {
        return response(
                false,
                false,
                false,
                false,
                "Execution host preflight has not run.",
                null
        );
    }

    private CodeExecutionPreflightResponse response(
            boolean checked,
            boolean hostReady,
            boolean dockerAvailable,
            boolean imageReady,
            String message,
            Instant checkedAt
    ) {
        return new CodeExecutionPreflightResponse(
                properties.isRequireOsIsolation(),
                checked,
                hostReady,
                dockerAvailable,
                imageReady,
                properties.isDockerPrePull(),
                message,
                checkedAt
        );
    }

    private boolean dockerMode() {
        return properties.getMode()
                == CodeExecutionProperties.ExecutionMode.DOCKER;
    }

    @FunctionalInterface
    interface CommandExecutor {
        boolean run(List<String> command, int timeoutSeconds);
    }

    private static final class ProcessCommandExecutor implements CommandExecutor {

        @Override
        public boolean run(List<String> command, int timeoutSeconds) {
            try {
                Process process = new ProcessBuilder(command)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                return new CodeExecutionProcessWaiter().waitFor(process, timeoutSeconds, command)
                        && process.exitValue() == 0;
            } catch (IOException exception) {
                return false;
            }
        }
    }
}
