package intercoach.submission.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "intercoach.review-queue.enabled", havingValue = "true")
@EnableScheduling
public class ReviewQueueSchedulingConfig {
}
