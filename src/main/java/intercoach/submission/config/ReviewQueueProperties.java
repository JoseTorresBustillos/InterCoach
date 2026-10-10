package intercoach.submission.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "intercoach.review-queue")
@Validated
public class ReviewQueueProperties {

    @Min(1)
    private int maxAttempts = 3;

    @Min(1)
    private int leaseSeconds = 300;

    @Min(1)
    private int retryDelaySeconds = 30;

    @Min(250)
    private int pollIntervalMs = 5000;

    @Min(1)
    private int connectTimeoutSeconds = 5;

    @Min(1)
    private int responseTimeoutSeconds = 90;

    @AssertTrue(message = "lease must cover connect and response timeouts plus 15 seconds for completion")
    public boolean isLeaseBudgetValid() {
        return leaseSeconds >= (long) connectTimeoutSeconds + responseTimeoutSeconds + 15;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getResponseTimeoutSeconds() {
        return responseTimeoutSeconds;
    }

    public void setResponseTimeoutSeconds(int responseTimeoutSeconds) {
        this.responseTimeoutSeconds = responseTimeoutSeconds;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getLeaseSeconds() {
        return leaseSeconds;
    }

    public void setLeaseSeconds(int leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }

    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public int getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(int pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }
}
