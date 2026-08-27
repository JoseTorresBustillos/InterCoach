package intercoach.service;

import intercoach.dto.CodeExecutionResponse;
import intercoach.dto.CodeExecutionRuntimeStatsResponse;
import intercoach.dto.CodeExecutionStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Service
public class CodeExecutionRunMonitor {

    private final LongAdder totalRuns = new LongAdder();
    private final LongAdder successfulRuns = new LongAdder();
    private final LongAdder compileErrorRuns = new LongAdder();
    private final LongAdder runtimeErrorRuns = new LongAdder();
    private final LongAdder timeoutRuns = new LongAdder();
    private final LongAdder wrongAnswerRuns = new LongAdder();
    private final LongAdder unsupportedLanguageRuns = new LongAdder();
    private final LongAdder sourceTooLargeRuns = new LongAdder();
    private final LongAdder noTestRuns = new LongAdder();
    private final LongAdder totalDurationMs = new LongAdder();
    private final AtomicLong lastDurationMs = new AtomicLong();
    private final AtomicReference<CodeExecutionStatus> lastStatus =
            new AtomicReference<>();
    private final AtomicReference<Instant> lastRunAt = new AtomicReference<>();

    public void record(CodeExecutionResponse response) {
        CodeExecutionStatus status = response.status();
        long durationMs = Math.max(0, response.durationMs());

        totalRuns.increment();
        totalDurationMs.add(durationMs);
        lastDurationMs.set(durationMs);
        lastStatus.set(status);
        lastRunAt.set(Instant.now());

        switch (status) {
            case SUCCESS -> successfulRuns.increment();
            case COMPILE_ERROR -> compileErrorRuns.increment();
            case RUNTIME_ERROR -> runtimeErrorRuns.increment();
            case TIME_LIMIT_EXCEEDED -> timeoutRuns.increment();
            case WRONG_ANSWER -> wrongAnswerRuns.increment();
            case UNSUPPORTED_LANGUAGE -> unsupportedLanguageRuns.increment();
            case SOURCE_TOO_LARGE -> sourceTooLargeRuns.increment();
            case NO_TESTS -> noTestRuns.increment();
        }
    }

    public CodeExecutionRuntimeStatsResponse snapshot() {
        long total = totalRuns.sum();
        long successes = successfulRuns.sum();
        CodeExecutionStatus status = lastStatus.get();

        return new CodeExecutionRuntimeStatsResponse(
                total,
                successes,
                Math.max(0, total - successes),
                compileErrorRuns.sum(),
                runtimeErrorRuns.sum(),
                timeoutRuns.sum(),
                wrongAnswerRuns.sum(),
                unsupportedLanguageRuns.sum(),
                sourceTooLargeRuns.sum(),
                noTestRuns.sum(),
                total == 0 ? 0 : totalDurationMs.sum() / total,
                lastDurationMs.get(),
                status == null ? null : status.name(),
                lastRunAt.get()
        );
    }
}
