package intercoach.submission.config;

import intercoach.auth.security.UserAccessService;
import intercoach.submission.controller.QueuedSubmissionController;
import intercoach.submission.repository.ReviewJobRepository;
import intercoach.submission.repository.SubmissionRepository;
import intercoach.submission.service.AiFeedbackService;
import intercoach.submission.service.QueuedSubmissionService;
import intercoach.submission.service.ReviewJobCompletionService;
import intercoach.submission.service.ReviewJobWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReviewQueuePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(QueueConfiguration.class)
            .withBean(QueuedSubmissionService.class, () -> mock(QueuedSubmissionService.class))
            .withBean(UserAccessService.class, () -> mock(UserAccessService.class))
            .withBean(ReviewJobRepository.class, () -> mock(ReviewJobRepository.class))
            .withBean(SubmissionRepository.class, () -> mock(SubmissionRepository.class))
            .withBean(AiFeedbackService.class, () -> mock(AiFeedbackService.class))
            .withBean(ReviewJobCompletionService.class, () -> mock(ReviewJobCompletionService.class));

    @Test
    void queuedApiAndWorkerAreDisabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(QueuedSubmissionController.class);
            assertThat(context).doesNotHaveBean(ReviewJobWorker.class);
            ReviewQueueProperties properties = context.getBean(ReviewQueueProperties.class);
            assertThat(properties.getMaxAttempts()).isEqualTo(3);
            assertThat(properties.getLeaseSeconds()).isEqualTo(300);
            assertThat(properties.getRetryDelaySeconds()).isEqualTo(30);
            assertThat(properties.getPollIntervalMs()).isEqualTo(5000);
        });
    }

    @Test
    void enablingQueueCreatesApiAndWorkerAndBindsConfiguration() {
        runner.withPropertyValues("intercoach.review-queue.enabled=true",
                "intercoach.review-queue.max-attempts=5",
                "intercoach.review-queue.lease-seconds=600",
                "intercoach.review-queue.retry-delay-seconds=60",
                "intercoach.review-queue.poll-interval-ms=1000").run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(QueuedSubmissionController.class);
                    assertThat(context).hasSingleBean(ReviewJobWorker.class);
                    ReviewQueueProperties properties = context.getBean(ReviewQueueProperties.class);
                    assertThat(properties.getMaxAttempts()).isEqualTo(5);
                    assertThat(properties.getLeaseSeconds()).isEqualTo(600);
                    assertThat(properties.getRetryDelaySeconds()).isEqualTo(60);
                    assertThat(properties.getPollIntervalMs()).isEqualTo(1000);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"max-attempts=0", "max-attempts=-1", "lease-seconds=0",
            "retry-delay-seconds=0", "poll-interval-ms=0", "poll-interval-ms=249"})
    void invalidQueueLimitsFailStartup(String property) {
        runner.withPropertyValues("intercoach.review-queue." + property)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ReviewQueueProperties.class)
    @Import({QueuedSubmissionController.class, ReviewJobWorker.class})
    static class QueueConfiguration {
    }
}
