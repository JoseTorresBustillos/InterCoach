package intercoach.service;

import intercoach.config.CodeExecutionProperties;
import intercoach.dto.CodeExecutionPreflightResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CodeExecutionHostPreflight implements ApplicationRunner {

    private static final String DOCKER_COMMAND = "docker";

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
            ready(true, true, "Docker host and runner image are ready.");
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

        ready(true, true, "Docker runner image was pre-pulled and is ready.");
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
            Process process = null;

            try {
                process = new ProcessBuilder(command)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                boolean completed = process.waitFor(
                        timeoutSeconds,
                        TimeUnit.SECONDS
                );

                if (!completed) {
                    process.destroyForcibly();
                    return false;
                }

                return process.exitValue() == 0;
            } catch (IOException exception) {
                return false;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                if (process != null) {
                    process.destroyForcibly();
                }

                return false;
            }
        }
    }
}
