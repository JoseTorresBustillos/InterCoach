package intercoach.execution.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CodeExecutionProcessWaiter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CodeExecutionProcessWaiter.class);
    private static final int PROCESS_EXIT_TIMEOUT_SECONDS = 1;
    private static final int CONTAINER_REMOVAL_TIMEOUT_SECONDS = 5;

    private final ProcessStarter processStarter;

    public CodeExecutionProcessWaiter() {
        this(ProcessBuilder::start);
    }

    CodeExecutionProcessWaiter(ProcessStarter processStarter) {
        this.processStarter = processStarter;
    }

    public boolean waitFor(Process process, int seconds, List<String> command) {
        boolean completed = false;
        boolean interrupted = false;

        try {
            completed = process.waitFor(seconds, TimeUnit.SECONDS);
            return completed;
        } catch (InterruptedException exception) {
            interrupted = true;
            return false;
        } finally {
            if (!completed) {
                interrupted |= terminate(process);
                String containerName = containerName(command);
                if (containerName != null) {
                    // Killing the attached Docker client does not stop the container.
                    interrupted |= removeContainer(containerName);
                }
            }
            // Defer restoration until bounded cleanup can finish its blocking waits.
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String containerName(List<String> command) {
        if (command.size() < 2
                || !"docker".equals(command.get(0))
                || !"run".equals(command.get(1))) {
            return null;
        }
        int nameIndex = command.indexOf("--name");
        return nameIndex >= 0 && nameIndex + 1 < command.size()
                ? command.get(nameIndex + 1)
                : null;
    }

    private boolean removeContainer(String containerName) {
        Process cleanup = null;
        boolean interrupted = false;

        try {
            cleanup = processStarter.start(new ProcessBuilder(
                    "docker", "rm", "--force", containerName
            ).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD));
            if (!cleanup.waitFor(CONTAINER_REMOVAL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOGGER.warn("Docker container removal timed out for {}", containerName);
            } else if (cleanup.exitValue() != 0) {
                LOGGER.warn("Docker container removal could not be confirmed for {}",
                        containerName);
            }
        } catch (IOException exception) {
            LOGGER.warn("Docker container removal could not start for {}", containerName,
                    exception);
        } catch (InterruptedException exception) {
            interrupted = true;
            LOGGER.warn("Docker container removal was interrupted for {}", containerName);
        } finally {
            if (cleanup != null && cleanup.isAlive()) {
                interrupted |= terminate(cleanup);
            }
        }
        return interrupted;
    }

    private boolean terminate(Process process) {
        process.destroyForcibly();
        try {
            if (!process.waitFor(PROCESS_EXIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOGGER.warn("Execution client did not exit after forced termination");
            }
            return false;
        } catch (InterruptedException exception) {
            return true;
        }
    }

    @FunctionalInterface
    interface ProcessStarter {
        Process start(ProcessBuilder builder) throws IOException;
    }
}
