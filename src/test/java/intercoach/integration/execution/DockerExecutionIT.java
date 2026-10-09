package intercoach.integration.execution;

import intercoach.execution.config.CodeExecutionProperties;
import intercoach.execution.dto.CodeExecutionRequest;
import intercoach.execution.dto.CodeExecutionResponse;
import intercoach.execution.dto.CodeExecutionStatus;
import intercoach.execution.service.CodeExecutionProcessWaiter;
import intercoach.execution.service.CodeExecutionRunMonitor;
import intercoach.execution.service.CodeExecutionService;
import intercoach.problem.model.TestCase;
import intercoach.problem.repository.ProblemRepository;
import intercoach.problem.repository.TestCaseRepository;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.DockerClientFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@EnabledIf("dockerAvailableOrRequired")
@Timeout(60)
class DockerExecutionIT {

    private static final String RUNNER_IMAGE = "eclipse-temurin:21-jdk";

    private final List<String> containers = new ArrayList<>();

    static boolean dockerAvailableOrRequired() {
        return Boolean.getBoolean("intercoach.tests.require-docker")
                || DockerClientFactory.instance().isDockerAvailable();
    }

    @BeforeAll
    static void prepareRunnerImage() throws Exception {
        assertThat(dockerCommand(List.of("docker", "info"), 10)).isTrue();
        if (!dockerCommand(List.of("docker", "image", "inspect", RUNNER_IMAGE), 10)) {
            assertThat(dockerCommand(List.of("docker", "pull", RUNNER_IMAGE), 120))
                    .as("The Java runner image must be available for execution tests")
                    .isTrue();
        }
    }

    @AfterEach
    void removeTestContainers() throws Exception {
        for (String name : containers) {
            dockerCommand(List.of("docker", "rm", "--force", name), 10);
        }
    }

    @Test
    void dockerRunnerCompilesJavaAndDeliversStdinWithoutExposingHiddenTests() {
        ProblemRepository problems = mock(ProblemRepository.class);
        TestCaseRepository testCases = mock(TestCaseRepository.class);
        given(problems.existsById(1L)).willReturn(true);
        given(testCases.findByProblemId(1L)).willReturn(List.of(
                testCase(10L, "hello", "hello", false),
                testCase(11L, "hidden", "incorrect", true)
        ));
        CodeExecutionProperties properties = new CodeExecutionProperties();
        properties.setMode(CodeExecutionProperties.ExecutionMode.DOCKER);
        properties.setDockerImage(RUNNER_IMAGE);
        properties.setCompileTimeoutSeconds(20);
        properties.setTestTimeoutSeconds(10);
        CodeExecutionService service = new CodeExecutionService(
                problems, testCases, properties, new CodeExecutionRunMonitor()
        );
        CodeExecutionRequest request = new CodeExecutionRequest();
        ReflectionTestUtils.setField(request, "language", "Java");
        ReflectionTestUtils.setField(request, "submittedCode", """
                public class Main {
                    public static void main(String[] args) throws Exception {
                        System.out.write(System.in.readAllBytes());
                    }
                }
                """);

        CodeExecutionResponse response = service.runCode(1L, request);

        assertThat(response.status()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(response.totalTests()).isEqualTo(1);
        assertThat(response.testCases().getFirst().testCaseId()).isEqualTo(10L);
        assertThat(response.testCases().getFirst().actualOutput()).isEqualTo("hello");
    }

    @Test
    void timeoutRemovesTheContainerInsteadOfOnlyKillingTheClient() throws Exception {
        List<String> command = sleepingContainerCommand();
        String name = command.get(command.indexOf("--name") + 1);
        Process client = start(command);
        try {
            awaitContainer(name);

            assertThat(new CodeExecutionProcessWaiter().waitFor(client, 1, command)).isFalse();

            assertContainerRemoved(name);
            assertThat(client.isAlive()).isFalse();
        } finally {
            client.destroyForcibly();
        }
    }

    @Test
    void interruptedWaitRemovesTheContainerAndRestoresTheThreadFlag() throws Exception {
        List<String> command = sleepingContainerCommand();
        String name = command.get(command.indexOf("--name") + 1);
        Process client = start(command);
        AtomicBoolean completed = new AtomicBoolean(true);
        AtomicBoolean interrupted = new AtomicBoolean(false);
        Thread worker = new Thread(() -> {
            completed.set(new CodeExecutionProcessWaiter().waitFor(client, 30, command));
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        try {
            awaitContainer(name);
            worker.start();
            worker.interrupt();
            worker.join(Duration.ofSeconds(10));

            assertThat(worker.isAlive()).isFalse();
            assertThat(completed.get()).isFalse();
            assertThat(interrupted.get()).isTrue();
            assertContainerRemoved(name);
            assertThat(client.isAlive()).isFalse();
        } finally {
            client.destroyForcibly();
            worker.interrupt();
            worker.join(Duration.ofSeconds(10));
        }
    }

    private List<String> sleepingContainerCommand() {
        String name = "intercoach-execution-it-" + UUID.randomUUID();
        containers.add(name);
        return List.of(
                "docker", "run", "--rm", "--name", name,
                "--network", "none", "--memory", "128m", "--cpus", "1",
                "--pids-limit", "32", "--read-only", "--entrypoint", "sh",
                RUNNER_IMAGE, "-c", "sleep 60"
        );
    }

    private void awaitContainer(String name) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (dockerCommand(List.of("docker", "exec", name, "true"), 5)) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Test container did not appear: " + name);
    }

    private void assertContainerRemoved(String name) throws Exception {
        assertThat(dockerCommand(List.of("docker", "info"), 5))
                .as("The daemon must remain reachable when checking removal")
                .isTrue();
        assertThat(dockerCommand(List.of("docker", "inspect", name), 5))
                .as("Timed-out or interrupted container must no longer exist")
                .isFalse();
    }

    private static Process start(List<String> command) throws IOException {
        return new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
    }

    private static boolean dockerCommand(List<String> command, int seconds) throws Exception {
        Process process = start(command);
        try {
            assertThat(process.waitFor(seconds, TimeUnit.SECONDS))
                    .as("Docker command must finish within its test deadline: %s", command)
                    .isTrue();
            return process.exitValue() == 0;
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
            }
        }
    }

    private TestCase testCase(Long id, String input, String expected, boolean hidden) {
        TestCase testCase = new TestCase();
        ReflectionTestUtils.setField(testCase, "id", id);
        testCase.setInput(input);
        testCase.setExpectedOutput(expected);
        testCase.setHidden(hidden);
        return testCase;
    }
}
