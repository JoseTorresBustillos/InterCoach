package InterCoach.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "intercoach.execution")
@Validated
public class CodeExecutionProperties {

    @Min(1)
    private int compileTimeoutSeconds = 5;

    @Min(1)
    private int testTimeoutSeconds = 2;

    @Min(256)
    private int outputLimitCharacters = 4000;

    @Min(1000)
    private int maxSourceCharacters = 20000;

    @Min(16)
    private int maxHeapMegabytes = 128;

    @Min(1)
    private int activeProcessorCount = 1;

    public int getCompileTimeoutSeconds() {
        return compileTimeoutSeconds;
    }

    public void setCompileTimeoutSeconds(int compileTimeoutSeconds) {
        this.compileTimeoutSeconds = compileTimeoutSeconds;
    }

    public int getTestTimeoutSeconds() {
        return testTimeoutSeconds;
    }

    public void setTestTimeoutSeconds(int testTimeoutSeconds) {
        this.testTimeoutSeconds = testTimeoutSeconds;
    }

    public int getOutputLimitCharacters() {
        return outputLimitCharacters;
    }

    public void setOutputLimitCharacters(int outputLimitCharacters) {
        this.outputLimitCharacters = outputLimitCharacters;
    }

    public int getMaxSourceCharacters() {
        return maxSourceCharacters;
    }

    public void setMaxSourceCharacters(int maxSourceCharacters) {
        this.maxSourceCharacters = maxSourceCharacters;
    }

    public int getMaxHeapMegabytes() {
        return maxHeapMegabytes;
    }

    public void setMaxHeapMegabytes(int maxHeapMegabytes) {
        this.maxHeapMegabytes = maxHeapMegabytes;
    }

    public int getActiveProcessorCount() {
        return activeProcessorCount;
    }

    public void setActiveProcessorCount(int activeProcessorCount) {
        this.activeProcessorCount = activeProcessorCount;
    }
}
