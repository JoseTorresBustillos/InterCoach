package intercoach.service;

import intercoach.config.CodeExecutionProperties;
import intercoach.dto.CodeExecutionRequest;
import intercoach.dto.CodeExecutionResponse;
import intercoach.dto.CodeExecutionRuntimeStatsResponse;
import intercoach.dto.CodeExecutionStatus;
import intercoach.dto.CodeExecutionTestCaseStatus;
import intercoach.exception.ResourceNotFoundException;
import intercoach.model.TestCase;
import intercoach.repository.ProblemRepository;
import intercoach.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CodeExecutionServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    private CodeExecutionService codeExecutionService;
    private CodeExecutionRunMonitor runMonitor;
    private CodeExecutionProperties properties;

    @BeforeEach
    void setUp() {
        properties = executionProperties();
        runMonitor = new CodeExecutionRunMonitor();
        codeExecutionService = new CodeExecutionService(
                problemRepository,
                testCaseRepository,
                properties,
                runMonitor
        );
    }

    @Test
    void runCodeExecutesVisibleJavaTestCases() {
        given(problemRepository.existsById(1L)).willReturn(true);
        given(testCaseRepository.findByProblemId(1L))
                .willReturn(List.of(
                        testCase(10L, "hello", "hello\n", false),
                        testCase(11L, "hidden", "hidden\n", true)
                ));

        CodeExecutionResponse response =
                codeExecutionService.runCode(1L, request("""
                        public class Main {
                            public static void main(String[] args) {
                                java.util.Scanner scanner =
                                        new java.util.Scanner(System.in);
                                System.out.println(scanner.nextLine());
                            }
                        }
                        """, "Java"));

        assertThat(response.status()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(response.allPassed()).isTrue();
        assertThat(response.passedTests()).isEqualTo(1);
        assertThat(response.totalTests()).isEqualTo(1);
        assertThat(response.testCases()).hasSize(1);
        assertThat(response.testCases().getFirst().testCaseId()).isEqualTo(10L);
        assertThat(response.testCases().getFirst().status())
                .isEqualTo(CodeExecutionTestCaseStatus.PASSED);
    }

    @Test
    void runCodeReportsWrongAnswer() {
        given(problemRepository.existsById(1L)).willReturn(true);
        given(testCaseRepository.findByProblemId(1L))
                .willReturn(List.of(testCase(10L, "hello", "expected", false)));

        CodeExecutionResponse response =
                codeExecutionService.runCode(1L, request("""
                        public class Main {
                            public static void main(String[] args) {
                                System.out.println("actual");
                            }
                        }
                        """, "Java"));

        assertThat(response.status()).isEqualTo(CodeExecutionStatus.WRONG_ANSWER);
        assertThat(response.allPassed()).isFalse();
        assertThat(response.testCases().getFirst().actualOutput())
                .isEqualTo("actual\n");
        assertThat(response.testCases().getFirst().status())
                .isEqualTo(CodeExecutionTestCaseStatus.WRONG_ANSWER);
    }

    @Test
    void runCodeReportsCompileErrors() {
        given(problemRepository.existsById(1L)).willReturn(true);
        given(testCaseRepository.findByProblemId(1L))
                .willReturn(List.of(testCase(10L, "hello", "hello", false)));

        CodeExecutionResponse response =
                codeExecutionService.runCode(1L, request("""
                        public class Main {
                            public static void main(String[] args) {
                                System.out.println("missing semicolon")
                            }
                        }
                        """, "Java"));

        assertThat(response.status()).isEqualTo(CodeExecutionStatus.COMPILE_ERROR);
        assertThat(response.compileOutput()).contains("error");
        assertThat(response.testCases()).isEmpty();
    }

    @Test
    void runCodeRejectsUnsupportedLanguages() {
        given(problemRepository.existsById(1L)).willReturn(true);

        CodeExecutionResponse response =
                codeExecutionService.runCode(
                        1L,
                        request("print('hello')", "Python")
                );

        assertThat(response.status())
                .isEqualTo(CodeExecutionStatus.UNSUPPORTED_LANGUAGE);
        assertThat(response.compileOutput())
                .contains("Only Java execution is currently supported");
    }

    @Test
    void runCodeRecordsExecutionStatistics() {
        given(problemRepository.existsById(1L)).willReturn(true);
        given(testCaseRepository.findByProblemId(1L))
                .willReturn(List.of(testCase(10L, "hello", "expected", false)));

        codeExecutionService.runCode(
                1L,
                request("System.out.println('bad');", "Python")
        );
        codeExecutionService.runCode(1L, request("""
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("actual");
                    }
                }
                """, "Java"));
        CodeExecutionRuntimeStatsResponse snapshot = runMonitor.snapshot();

        assertThat(snapshot.totalRuns()).isEqualTo(2);
        assertThat(snapshot.failedRuns()).isEqualTo(2);
        assertThat(snapshot.unsupportedLanguageRuns()).isEqualTo(1);
        assertThat(snapshot.wrongAnswerRuns()).isEqualTo(1);
        assertThat(snapshot.lastStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(snapshot.lastRunAt()).isNotNull();
    }

    @Test
    void runCodeRejectsOversizedSourceBeforeLoadingTestCases() {
        properties.setMaxSourceCharacters(20);
        given(problemRepository.existsById(1L)).willReturn(true);

        CodeExecutionResponse response =
                codeExecutionService.runCode(
                        1L,
                        request("public class Main { }", "Java")
                );

        assertThat(response.status()).isEqualTo(CodeExecutionStatus.SOURCE_TOO_LARGE);
        assertThat(response.compileOutput()).contains("20 character limit");
        assertThat(response.testCases()).isEmpty();
    }

    @Test
    void runCodeAppliesConfiguredJvmHeapLimit() {
        properties.setMaxHeapMegabytes(32);
        given(problemRepository.existsById(1L)).willReturn(true);
        given(testCaseRepository.findByProblemId(1L))
                .willReturn(List.of(testCase(10L, "", "true\n", false)));

        CodeExecutionResponse response =
                codeExecutionService.runCode(1L, request("""
                        public class Main {
                            public static void main(String[] args) {
                                long maxMemoryMb =
                                        Runtime.getRuntime().maxMemory()
                                                / 1024
                                                / 1024;
                                System.out.println(maxMemoryMb <= 64);
                            }
                        }
                        """, "Java"));

        assertThat(response.status()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(response.testCases().getFirst().actualOutput())
                .isEqualTo("true\n");
    }

    @Test
    void runCodeTruncatesOversizedOutput() {
        properties.setOutputLimitCharacters(10);
        given(problemRepository.existsById(1L)).willReturn(true);
        given(testCaseRepository.findByProblemId(1L))
                .willReturn(List.of(testCase(10L, "", "expected", false)));

        CodeExecutionResponse response =
                codeExecutionService.runCode(1L, request("""
                        public class Main {
                            public static void main(String[] args) {
                                System.out.print("abcdefghijklmnop");
                            }
                        }
                        """, "Java"));

        assertThat(response.status()).isEqualTo(CodeExecutionStatus.WRONG_ANSWER);
        assertThat(response.testCases().getFirst().actualOutput())
                .isEqualTo("abcdefghij\n... output truncated ...");
    }

    @Test
    void runCodeUsesConfiguredTestTimeout() {
        properties.setTestTimeoutSeconds(1);
        given(problemRepository.existsById(1L)).willReturn(true);
        given(testCaseRepository.findByProblemId(1L))
                .willReturn(List.of(testCase(10L, "", "done", false)));

        CodeExecutionResponse response =
                codeExecutionService.runCode(1L, request("""
                        public class Main {
                            public static void main(String[] args) {
                                while (true) {
                                }
                            }
                        }
                        """, "Java"));

        assertThat(response.status())
                .isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
        assertThat(response.testCases().getFirst().status())
                .isEqualTo(CodeExecutionTestCaseStatus.TIME_LIMIT_EXCEEDED);
        assertThat(response.testCases().getFirst().errorOutput())
                .contains("1 seconds");
    }

    @Test
    void compileCommandUsesLocalJavaToolchainByDefault() {
        List<String> command =
                codeExecutionService.compileCommand(Path.of("/tmp/workspace"));

        assertThat(command)
                .containsExactly(
                        "javac",
                        "-J-Xmx128m",
                        "-J-XX:ActiveProcessorCount=1",
                        "-proc:none",
                        "Main.java"
                );
    }

    @Test
    void compileCommandCanRunInsideDockerSandbox() {
        properties.setMode(CodeExecutionProperties.ExecutionMode.DOCKER);
        properties.setDockerImage("eclipse-temurin:21-jdk");
        properties.setDockerCpuCount(2);
        properties.setDockerMemoryMegabytes(384);
        properties.setDockerPidsLimit(48);
        properties.setDockerTmpfsMegabytes(32);

        List<String> command =
                codeExecutionService.compileCommand(Path.of("/tmp/workspace"));

        assertThat(command)
                .containsSequence(
                        "docker",
                        "run",
                        "--rm",
                        "--network",
                        "none"
                )
                .contains(
                        "--cpus",
                        "2",
                        "--memory",
                        "384m",
                        "--pids-limit",
                        "48",
                        "--read-only",
                        "--tmpfs",
                        "/tmp:rw,nosuid,nodev,size=32m",
                        "-v",
                        "/tmp/workspace:/workspace:rw",
                        "-w",
                        "/workspace",
                        "eclipse-temurin:21-jdk",
                        "javac",
                        "Main.java"
                );
    }

    @Test
    void testCommandCanRunInsideDockerSandbox() {
        properties.setMode(CodeExecutionProperties.ExecutionMode.DOCKER);

        List<String> command =
                codeExecutionService.testCommand(Path.of("/tmp/workspace"));

        assertThat(command)
                .contains(
                        "docker",
                        "run",
                        "--network",
                        "none",
                        "--read-only",
                        "java",
                        "-Djava.io.tmpdir=/tmp",
                        "-Duser.home=/tmp",
                        "Main"
                );
    }

    @Test
    void runCodeRejectsUnknownProblems() {
        given(problemRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() ->
                codeExecutionService.runCode(
                        99L,
                        request("public class Main {}", "Java")
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Problem not found with id: 99");
    }

    private CodeExecutionProperties executionProperties() {
        return new CodeExecutionProperties();
    }

    private CodeExecutionRequest request(String submittedCode, String language) {
        CodeExecutionRequest request = new CodeExecutionRequest();

        ReflectionTestUtils.setField(request, "submittedCode", submittedCode);
        ReflectionTestUtils.setField(request, "language", language);

        return request;
    }

    private TestCase testCase(
            Long id,
            String input,
            String expectedOutput,
            boolean hidden
    ) {
        TestCase testCase = new TestCase();

        ReflectionTestUtils.setField(testCase, "id", id);
        testCase.setInput(input);
        testCase.setExpectedOutput(expectedOutput);
        testCase.setHidden(hidden);

        return testCase;
    }
}
