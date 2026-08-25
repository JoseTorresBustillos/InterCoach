package intercoach.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "intercoach.execution")
@Validated
public class CodeExecutionProperties {

    public enum ExecutionMode {
        LOCAL,
        DOCKER
    }

    private ExecutionMode mode = ExecutionMode.LOCAL;

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

    @NotBlank
    private String dockerImage = "eclipse-temurin:21-jdk";

    @Min(1)
    private int dockerCpuCount = 1;

    @Min(64)
    private int dockerMemoryMegabytes = 256;

    @Min(16)
    private int dockerTmpfsMegabytes = 64;

    @Min(1)
    private int dockerPidsLimit = 64;

    public ExecutionMode getMode() {
        return mode;
    }

    public void setMode(ExecutionMode mode) {
        this.mode = mode;
    }

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

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    public int getDockerCpuCount() {
        return dockerCpuCount;
    }

    public void setDockerCpuCount(int dockerCpuCount) {
        this.dockerCpuCount = dockerCpuCount;
    }

    public int getDockerMemoryMegabytes() {
        return dockerMemoryMegabytes;
    }

    public void setDockerMemoryMegabytes(int dockerMemoryMegabytes) {
        this.dockerMemoryMegabytes = dockerMemoryMegabytes;
    }

    public int getDockerTmpfsMegabytes() {
        return dockerTmpfsMegabytes;
    }

    public void setDockerTmpfsMegabytes(int dockerTmpfsMegabytes) {
        this.dockerTmpfsMegabytes = dockerTmpfsMegabytes;
    }

    public int getDockerPidsLimit() {
        return dockerPidsLimit;
    }

    public void setDockerPidsLimit(int dockerPidsLimit) {
        this.dockerPidsLimit = dockerPidsLimit;
    }
}
