package intercoach.execution.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CodeExecutionProcessWaiterTest {

    private static final List<String> DOCKER_COMMAND = List.of(
            "docker", "run", "--rm", "--name", "intercoach-run-test",
            "eclipse-temurin:21-jdk", "java", "Main"
    );

    @Mock
    private Process process;

    @Mock
    private Process removal;

    @Mock
    private CodeExecutionProcessWaiter.ProcessStarter processStarter;

    private CodeExecutionProcessWaiter waiter;

    @BeforeEach
    void setUp() {
        waiter = new CodeExecutionProcessWaiter(processStarter);
    }

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void completedDockerProcessNeedsNoExplicitRemoval() throws Exception {
        given(process.waitFor(2, TimeUnit.SECONDS)).willReturn(true);

        assertThat(waiter.waitFor(process, 2, DOCKER_COMMAND)).isTrue();

        verify(process, never()).destroyForcibly();
        verifyNoInteractions(processStarter);
    }

    @Test
    void timedOutDockerProcessRemovesOnlyItsNamedContainer() throws Exception {
        given(process.waitFor(2, TimeUnit.SECONDS)).willReturn(false);
        given(process.waitFor(1, TimeUnit.SECONDS)).willReturn(true);
        given(processStarter.start(any())).willAnswer(invocation -> {
            ProcessBuilder builder = invocation.getArgument(0);
            assertThat(builder.command()).containsExactly(
                    "docker", "rm", "--force", "intercoach-run-test"
            );
            assertThat(builder.redirectOutput()).isEqualTo(ProcessBuilder.Redirect.DISCARD);
            assertThat(builder.redirectError()).isEqualTo(ProcessBuilder.Redirect.DISCARD);
            return removal;
        });
        given(removal.waitFor(5, TimeUnit.SECONDS)).willReturn(true);

        assertThat(waiter.waitFor(process, 2, DOCKER_COMMAND)).isFalse();

        verify(process).destroyForcibly();
        verify(removal, never()).destroyForcibly();
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    void interruptedRunFinishesCleanupBeforeRestoringInterruptFlag() throws Exception {
        given(process.waitFor(2, TimeUnit.SECONDS)).willThrow(new InterruptedException());
        given(process.waitFor(1, TimeUnit.SECONDS)).willReturn(true);
        given(processStarter.start(any())).willAnswer(invocation -> {
            assertThat(Thread.currentThread().isInterrupted()).isFalse();
            return removal;
        });
        given(removal.waitFor(5, TimeUnit.SECONDS)).willReturn(true);

        assertThat(waiter.waitFor(process, 2, DOCKER_COMMAND)).isFalse();

        verify(processStarter).start(any());
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    void unavailableDockerCleanupDoesNotHideTheTimeout() throws Exception {
        given(process.waitFor(2, TimeUnit.SECONDS)).willReturn(false);
        given(process.waitFor(1, TimeUnit.SECONDS)).willReturn(true);
        given(processStarter.start(any())).willThrow(new IOException("Docker unavailable"));

        assertThat(waiter.waitFor(process, 2, DOCKER_COMMAND)).isFalse();
        verify(process).destroyForcibly();
    }

    @Test
    void stuckRemovalClientIsKilledWithinItsOwnDeadline() throws Exception {
        given(process.waitFor(2, TimeUnit.SECONDS)).willReturn(false);
        given(process.waitFor(1, TimeUnit.SECONDS)).willReturn(true);
        given(processStarter.start(any())).willReturn(removal);
        given(removal.waitFor(5, TimeUnit.SECONDS)).willReturn(false);
        given(removal.isAlive()).willReturn(true);
        given(removal.waitFor(1, TimeUnit.SECONDS)).willReturn(true);

        assertThat(waiter.waitFor(process, 2, DOCKER_COMMAND)).isFalse();

        verify(removal).waitFor(5, TimeUnit.SECONDS);
        verify(removal).destroyForcibly();
        verify(removal).waitFor(1, TimeUnit.SECONDS);
    }

    @Test
    void interruptionDuringRemovalIsRestoredAfterKillingCleanupClient() throws Exception {
        given(process.waitFor(2, TimeUnit.SECONDS)).willReturn(false);
        given(process.waitFor(1, TimeUnit.SECONDS)).willReturn(true);
        given(processStarter.start(any())).willReturn(removal);
        given(removal.waitFor(5, TimeUnit.SECONDS)).willThrow(new InterruptedException());
        given(removal.isAlive()).willReturn(true);
        given(removal.waitFor(1, TimeUnit.SECONDS)).willReturn(true);

        assertThat(waiter.waitFor(process, 2, DOCKER_COMMAND)).isFalse();

        verify(removal).destroyForcibly();
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    void failedRemovalDoesNotChangeTheExecutionResult() throws Exception {
        given(process.waitFor(2, TimeUnit.SECONDS)).willReturn(false);
        given(process.waitFor(1, TimeUnit.SECONDS)).willReturn(true);
        given(processStarter.start(any())).willReturn(removal);
        given(removal.waitFor(5, TimeUnit.SECONDS)).willReturn(true);
        given(removal.exitValue()).willReturn(1);

        assertThat(waiter.waitFor(process, 2, DOCKER_COMMAND)).isFalse();
    }

    @Test
    void localTimeoutDoesNotInvokeDockerEvenWithANameArgument() throws Exception {
        given(process.waitFor(2, TimeUnit.SECONDS)).willReturn(false);
        given(process.waitFor(1, TimeUnit.SECONDS)).willReturn(true);

        assertThat(waiter.waitFor(process, 2, List.of(
                "java", "Main", "--name", "unrelated-container"
        ))).isFalse();

        verify(process).destroyForcibly();
        verifyNoInteractions(processStarter);
    }
}
