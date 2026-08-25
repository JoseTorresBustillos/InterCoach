package intercoach.service;

import intercoach.config.CodeExecutionProperties;
import intercoach.dto.CodeExecutionRequest;
import intercoach.dto.CodeExecutionResponse;
import intercoach.dto.CodeExecutionStatus;
import intercoach.dto.CodeExecutionTestCaseResponse;
import intercoach.dto.CodeExecutionTestCaseStatus;
import intercoach.exception.ResourceNotFoundException;
import intercoach.model.TestCase;
import intercoach.repository.ProblemRepository;
import intercoach.repository.TestCaseRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

    private static final String JAVA_LANGUAGE = "Java";
    private static final String MAIN_CLASS = "Main";
    private static final String SOURCE_FILE = MAIN_CLASS + ".java";

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final CodeExecutionProperties properties;

    public CodeExecutionService(
            ProblemRepository problemRepository,
            TestCaseRepository testCaseRepository,
            CodeExecutionProperties properties
    ) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.properties = properties;
    }

    public CodeExecutionResponse runCode(
            Long problemId,
            CodeExecutionRequest request
    ) {
        if (!problemRepository.existsById(problemId)) {
            throw new ResourceNotFoundException(
                    "Problem not found with id: " + problemId
            );
        }

        if (!JAVA_LANGUAGE.equalsIgnoreCase(request.getLanguage())) {
            return unsupportedLanguageResponse(problemId, request.getLanguage());
        }

        if (request.getSubmittedCode().length()
                > properties.getMaxSourceCharacters()) {
            return sourceTooLargeResponse(problemId);
        }

        // User-triggered runs execute only visible cases so hidden cases stay hidden.
        List<TestCase> visibleTestCases = testCaseRepository
                .findByProblemId(problemId)
                .stream()
                .filter(testCase -> !testCase.isHidden())
                .toList();

        if (visibleTestCases.isEmpty()) {
            return noTestsResponse(problemId);
        }

        Instant startedAt = Instant.now();
        Path workspace = null;

        try {
            // A fresh workspace keeps compiled artifacts isolated per request.
            workspace = Files.createTempDirectory("intercoach-code-run-");
            Files.writeString(
                    workspace.resolve(SOURCE_FILE),
                    request.getSubmittedCode(),
                    StandardCharsets.UTF_8
            );

            String compileOutput = compile(workspace);

            if (!compileOutput.isBlank()) {
                return new CodeExecutionResponse(
                        problemId,
                        CodeExecutionStatus.COMPILE_ERROR,
                        false,
                        0,
                        visibleTestCases.size(),
                        elapsedMs(startedAt),
                        compileOutput,
                        List.of()
                );
            }

            Path executionWorkspace = workspace;
            List<CodeExecutionTestCaseResponse> results = visibleTestCases.stream()
                    .map(testCase -> runTestCase(executionWorkspace, testCase))
                    .toList();

            return executionResponse(
                    problemId,
                    elapsedMs(startedAt),
                    results
            );
        } catch (IOException exception) {
            return new CodeExecutionResponse(
                    problemId,
                    CodeExecutionStatus.RUNTIME_ERROR,
                    false,
                    0,
                    visibleTestCases.size(),
                    elapsedMs(startedAt),
                    "Code execution workspace could not be prepared: "
                            + exception.getMessage(),
                    List.of()
            );
        } finally {
            cleanup(workspace);
        }
    }

    private String compile(Path workspace) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(compileCommand(workspace))
                .directory(workspace.toFile())
                .redirectErrorStream(true)
                .redirectOutput(workspace.resolve("compile.out").toFile());

        sanitizeEnvironment(processBuilder);

        Process process = processBuilder.start();
        boolean completed = waitFor(
                process,
                properties.getCompileTimeoutSeconds()
        );

        if (!completed) {
            return "Compilation timed out after "
                    + properties.getCompileTimeoutSeconds()
                    + " seconds.";
        }

        String output = readLimited(workspace.resolve("compile.out"));

        return process.exitValue() == 0 ? "" : output;
    }

    private CodeExecutionTestCaseResponse runTestCase(
            Path workspace,
            TestCase testCase
    ) {
        Instant startedAt = Instant.now();
        Path outputFile = workspace.resolve("test-" + testCase.getId() + ".out");
        Path errorFile = workspace.resolve("test-" + testCase.getId() + ".err");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(testCommand(workspace))
                    .directory(workspace.toFile())
                    .redirectOutput(outputFile.toFile())
                    .redirectError(errorFile.toFile());

            sanitizeEnvironment(processBuilder);

            Process process = processBuilder.start();

            writeInput(process, testCase.getInput());

            boolean completed = waitFor(
                    process,
                    properties.getTestTimeoutSeconds()
            );
            long durationMs = elapsedMs(startedAt);

            if (!completed) {
                return testCaseResponse(
                        testCase,
                        "",
                        "Execution timed out after "
                                + properties.getTestTimeoutSeconds()
                                + " seconds.",
                        CodeExecutionTestCaseStatus.TIME_LIMIT_EXCEEDED,
                        durationMs
                );
            }

            String actualOutput = readLimited(outputFile);
            String errorOutput = readLimited(errorFile);

            if (process.exitValue() != 0) {
                return testCaseResponse(
                        testCase,
                        actualOutput,
                        errorOutput,
                        CodeExecutionTestCaseStatus.RUNTIME_ERROR,
                        durationMs
                );
            }

            boolean passed = normalized(actualOutput)
                    .equals(normalized(testCase.getExpectedOutput()));

            return testCaseResponse(
                    testCase,
                    actualOutput,
                    errorOutput,
                    passed
                            ? CodeExecutionTestCaseStatus.PASSED
                            : CodeExecutionTestCaseStatus.WRONG_ANSWER,
                    durationMs
            );
        } catch (IOException exception) {
            return testCaseResponse(
                    testCase,
                    "",
                    exception.getMessage(),
                    CodeExecutionTestCaseStatus.RUNTIME_ERROR,
                    elapsedMs(startedAt)
            );
        }
    }

    private CodeExecutionResponse executionResponse(
            Long problemId,
            long durationMs,
            List<CodeExecutionTestCaseResponse> results
    ) {
        int passedTests = (int) results.stream()
                .filter(CodeExecutionTestCaseResponse::passed)
                .count();
        CodeExecutionStatus status = overallStatus(results, passedTests);

        return new CodeExecutionResponse(
                problemId,
                status,
                status == CodeExecutionStatus.SUCCESS,
                passedTests,
                results.size(),
                durationMs,
                "",
                results
        );
    }

    private CodeExecutionStatus overallStatus(
            List<CodeExecutionTestCaseResponse> results,
            int passedTests
    ) {
        if (passedTests == results.size()) {
            return CodeExecutionStatus.SUCCESS;
        }

        if (results.stream().anyMatch(result ->
                result.status() == CodeExecutionTestCaseStatus.TIME_LIMIT_EXCEEDED
        )) {
            return CodeExecutionStatus.TIME_LIMIT_EXCEEDED;
        }

        if (results.stream().anyMatch(result ->
                result.status() == CodeExecutionTestCaseStatus.RUNTIME_ERROR
        )) {
            return CodeExecutionStatus.RUNTIME_ERROR;
        }

        return CodeExecutionStatus.WRONG_ANSWER;
    }

    private CodeExecutionTestCaseResponse testCaseResponse(
            TestCase testCase,
            String actualOutput,
            String errorOutput,
            CodeExecutionTestCaseStatus status,
            long durationMs
    ) {
        return new CodeExecutionTestCaseResponse(
                testCase.getId(),
                testCase.getInput(),
                testCase.getExpectedOutput(),
                actualOutput,
                status,
                status == CodeExecutionTestCaseStatus.PASSED,
                durationMs,
                errorOutput
        );
    }

    private CodeExecutionResponse unsupportedLanguageResponse(
            Long problemId,
            String language
    ) {
        return new CodeExecutionResponse(
                problemId,
                CodeExecutionStatus.UNSUPPORTED_LANGUAGE,
                false,
                0,
                0,
                0,
                "Only Java execution is currently supported. Received: "
                        + language,
                List.of()
        );
    }

    private CodeExecutionResponse sourceTooLargeResponse(Long problemId) {
        return new CodeExecutionResponse(
                problemId,
                CodeExecutionStatus.SOURCE_TOO_LARGE,
                false,
                0,
                0,
                0,
                "Submitted source exceeds the "
                        + properties.getMaxSourceCharacters()
                        + " character limit.",
                List.of()
        );
    }

    private CodeExecutionResponse noTestsResponse(Long problemId) {
        return new CodeExecutionResponse(
                problemId,
                CodeExecutionStatus.NO_TESTS,
                false,
                0,
                0,
                0,
                "",
                List.of()
        );
    }

    private void writeInput(Process process, String input) throws IOException {
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(value(input).getBytes(StandardCharsets.UTF_8));
        }
    }

    private boolean waitFor(Process process, int seconds) {
        try {
            boolean completed = process.waitFor(seconds, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
            }

            return completed;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return false;
        }
    }

    List<String> compileCommand(Path workspace) {
        List<String> command = baseExecutionCommand(workspace);

        command.addAll(List.of(
                "javac",
                "-J-Xmx" + properties.getMaxHeapMegabytes() + "m",
                "-J-XX:ActiveProcessorCount="
                        + properties.getActiveProcessorCount(),
                "-proc:none",
                SOURCE_FILE
        ));

        return command;
    }

    List<String> testCommand(Path workspace) {
        String tmpDirectory = dockerMode()
                ? "/tmp"
                : workspace.toAbsolutePath().toString();
        List<String> command = baseExecutionCommand(workspace);

        command.addAll(List.of(
                "java",
                "-Xmx" + properties.getMaxHeapMegabytes() + "m",
                "-XX:ActiveProcessorCount="
                        + properties.getActiveProcessorCount(),
                "-XX:+ExitOnOutOfMemoryError",
                "-Djava.io.tmpdir=" + tmpDirectory,
                "-Duser.home=" + tmpDirectory,
                "-cp",
                ".",
                MAIN_CLASS
        ));

        return command;
    }

    private List<String> baseExecutionCommand(Path workspace) {
        if (!dockerMode()) {
            return new ArrayList<>();
        }

        List<String> command = new ArrayList<>();
        command.addAll(List.of(
                "docker",
                "run",
                "--rm",
                "--network",
                "none",
                "--cpus",
                String.valueOf(properties.getDockerCpuCount()),
                "--memory",
                properties.getDockerMemoryMegabytes() + "m",
                "--pids-limit",
                String.valueOf(properties.getDockerPidsLimit()),
                "--read-only",
                "--tmpfs",
                "/tmp:rw,nosuid,nodev,size="
                        + properties.getDockerTmpfsMegabytes()
                        + "m",
                "-v",
                workspace.toAbsolutePath() + ":/workspace:rw",
                "-w",
                "/workspace",
                properties.getDockerImage()
        ));

        return command;
    }

    private boolean dockerMode() {
        return properties.getMode()
                == CodeExecutionProperties.ExecutionMode.DOCKER;
    }

    private void sanitizeEnvironment(ProcessBuilder processBuilder) {
        // Remove JVM injection hooks from the child process environment.
        processBuilder.environment().remove("CLASSPATH");
        processBuilder.environment().remove("JAVA_TOOL_OPTIONS");
        processBuilder.environment().remove("_JAVA_OPTIONS");
        processBuilder.environment().remove("JDK_JAVA_OPTIONS");
    }

    private String readLimited(Path path) throws IOException {
        if (!Files.exists(path)) {
            return "";
        }

        String output = Files.readString(path, StandardCharsets.UTF_8);

        if (output.length() <= properties.getOutputLimitCharacters()) {
            return output;
        }

        return output.substring(0, properties.getOutputLimitCharacters())
                + "\n... output truncated ...";
    }

    private String normalized(String output) {
        return value(output).stripTrailing();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private long elapsedMs(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }

    private void cleanup(Path workspace) {
        if (workspace == null) {
            return;
        }

        try (var paths = Files.walk(workspace)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup avoids hiding execution results.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup avoids hiding execution results.
        }
    }
}
