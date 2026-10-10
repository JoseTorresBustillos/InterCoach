package intercoach.integration.persistence;

import intercoach.problem.model.Problem;
import intercoach.problem.repository.ProblemRepository;
import intercoach.submission.config.ReviewQueueProperties;
import intercoach.submission.dto.AiFeedbackResponse;
import intercoach.submission.dto.SubmissionRequest;
import intercoach.submission.model.ReviewJobClaim;
import intercoach.submission.model.Submission;
import intercoach.submission.model.SubmissionStatus;
import intercoach.submission.repository.ReviewJobRepository;
import intercoach.submission.repository.SubmissionRepository;
import intercoach.submission.service.AiFeedbackService;
import intercoach.submission.service.QueuedAiFeedbackService;
import intercoach.submission.service.QueuedSubmissionService;
import intercoach.submission.service.ReviewJobCompletionService;
import intercoach.submission.service.ReviewJobWorker;
import intercoach.submission.service.SubmissionService;
import intercoach.user.model.AppUser;
import intercoach.user.repository.AppUserRepository;
import java.time.Duration;
import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "intercoach.review-queue.enabled=true",
        "spring.ai.openai.api-key=test-api-key",
        "spring.autoconfigure.exclude=org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration,"
                + "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration,"
                + "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration,"
                + "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration,"
                + "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration,"
                + "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration,"
                + "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration,"
                + "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ReviewQueueIT.QueueTestApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@EnabledIf("dockerAvailableOrRequired")
class ReviewQueueIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
    ).withStartupTimeout(Duration.ofMinutes(2));

    @Autowired private QueuedSubmissionService queued;
    @Autowired private ReviewJobCompletionService completion;
    @Autowired private ReviewJobWorker worker;
    @Autowired private ReviewQueueProperties properties;
    @Autowired private SubmissionRepository submissions;
    @Autowired private AppUserRepository users;
    @Autowired private ProblemRepository problems;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @MockitoSpyBean private ReviewJobRepository jobs;
    @MockitoBean private AiFeedbackService synchronousAi;
    @MockitoBean private QueuedAiFeedbackService ai;

    private Long userId;
    private Long problemId;

    static boolean dockerAvailableOrRequired() {
        return Boolean.getBoolean("intercoach.tests.require-docker")
                || DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {AppUser.class, Problem.class, Submission.class})
    @EnableJpaRepositories(basePackageClasses = {
            AppUserRepository.class, ProblemRepository.class, SubmissionRepository.class
    })
    @EnableConfigurationProperties(ReviewQueueProperties.class)
    @Import({SubmissionService.class, QueuedSubmissionService.class, ReviewJobRepository.class,
            ReviewJobCompletionService.class, ReviewJobWorker.class})
    static class QueueTestApplication {
    }

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM submission_review_job");
        jdbc.update("DELETE FROM submission");
        jdbc.update("DELETE FROM problem");
        jdbc.update("DELETE FROM app_user");
        properties.setMaxAttempts(3);
        properties.setRetryDelaySeconds(30);
        AppUser user = new AppUser();
        user.setUsername("queue-user");
        user.setEmail("queue@example.com");
        userId = users.saveAndFlush(user).getId();
        Problem problem = new Problem();
        problem.setTitle("Two Sum");
        problemId = problems.saveAndFlush(problem).getId();
    }

    @Test
    void enqueueCommitsPendingSubmissionAndJobWithoutCallingAi() {
        Long id = enqueue();
        assertThat(submission(id).getStatus()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(jobStatus(id)).isEqualTo("READY");
        verifyNoInteractions(ai);
    }

    @Test
    void workerCommitsStructuredFeedbackAndCompletedJob() {
        Long id = enqueue();
        when(ai.reviewSubmission(any(), any())).thenAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive()).isFalse();
            assertThat(((Problem) invocation.getArgument(0)).getTitle()).isEqualTo("Two Sum");
            return feedback("Useful review");
        });
        worker.runOnce();
        assertThat(jobStatus(id)).isEqualTo("COMPLETED");
        assertThat(submission(id).getStatus()).isEqualTo(SubmissionStatus.REVIEWED);
        assertThat(submission(id).getFeedbackSummary()).isEqualTo("Useful review");
        assertThat(submission(id).getTimeComplexity()).isEqualTo("O(n)");
    }

    @Test
    void failedProviderIsDelayedThenRetriedSuccessfully() {
        Long id = enqueue();
        when(ai.reviewSubmission(any(), any()))
                .thenThrow(new IllegalStateException("provider secret"))
                .thenReturn(feedback("Recovered"));
        worker.runOnce();
        assertThat(jobStatus(id)).isEqualTo("READY");
        assertThat(submission(id).getStatus()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(jobs.claim(3, 300)).isEmpty();
        jdbc.update("UPDATE submission_review_job SET available_at = CURRENT_TIMESTAMP - INTERVAL '1 second'");
        worker.runOnce();
        assertThat(jobStatus(id)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM submission_review_job", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void finalProviderFailureMarksSubmissionFailedWithoutExposingDetails() {
        properties.setMaxAttempts(1);
        Long id = enqueue();
        when(ai.reviewSubmission(any(), any())).thenThrow(new IllegalStateException("provider secret"));
        worker.runOnce();
        assertThat(jobStatus(id)).isEqualTo("FAILED");
        assertThat(submission(id).getStatus()).isEqualTo(SubmissionStatus.FAILED);
        assertThat(submission(id).getAiFeedback()).isEqualTo(ReviewJobRepository.FAILURE_MESSAGE);
    }

    @Test
    void expiredClaimCannotOverwriteReclaimedJob() {
        Long id = enqueue();
        ReviewJobClaim oldClaim = jobs.claim(3, 300).orElseThrow();
        expireLease();
        assertThat(completion.complete(oldClaim, feedback("Expired"))).isFalse();
        ReviewJobClaim newClaim = jobs.claim(3, 300).orElseThrow();
        assertThat(newClaim.token()).isNotEqualTo(oldClaim.token());
        assertThat(newClaim.attemptCount()).isEqualTo(2);
        completion.fail(oldClaim);
        assertThat(jobStatus(id)).isEqualTo("RUNNING");
        assertThat(completion.complete(newClaim, feedback("Current"))).isTrue();
        assertThat(completion.complete(oldClaim, feedback("Stale"))).isFalse();
        assertThat(submission(id).getFeedbackSummary()).isEqualTo("Current");
    }

    @Test
    void crashOnFinalAttemptIsRecoveredAsTerminalFailure() {
        properties.setMaxAttempts(1);
        Long id = enqueue();
        jobs.claim(1, 300).orElseThrow();
        expireLease();
        worker.runOnce();
        assertThat(jobStatus(id)).isEqualTo("FAILED");
        assertThat(submission(id).getStatus()).isEqualTo(SubmissionStatus.FAILED);
        verifyNoInteractions(ai);
    }

    @Test
    void concurrentWorkersClaimDifferentJobs() throws Exception {
        Long firstId = enqueue();
        Long secondId = enqueue();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return jobs.claim(3, 300).orElseThrow();
            });
            var second = executor.submit(() -> {
                start.await();
                return jobs.claim(3, 300).orElseThrow();
            });
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS).submissionId(),
                    second.get(10, TimeUnit.SECONDS).submissionId()))
                    .containsExactlyInAnyOrder(firstId, secondId);
        }
        assertThat(jobs.claim(3, 300)).isEmpty();
    }

    @Test
    void workerSkipsJobLockedByAnotherTransaction() throws Exception {
        Long lockedId = enqueue();
        Long availableId = enqueue();
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement(
                    "SELECT submission_id FROM submission_review_job WHERE submission_id = ? FOR UPDATE")) {
                statement.setLong(1, lockedId);
                try (var rows = statement.executeQuery()) {
                    assertThat(rows.next()).isTrue();
                    try (var executor = Executors.newSingleThreadExecutor()) {
                        var claimed = executor.submit(() -> jobs.claim(3, 300).orElseThrow());
                        assertThat(claimed.get(5, TimeUnit.SECONDS).submissionId()).isEqualTo(availableId);
                    }
                }
            } finally {
                connection.rollback();
            }
        }
        assertThat(jobs.claim(3, 300).orElseThrow().submissionId()).isEqualTo(lockedId);
    }

    @Test
    void enqueueFailureRollsBackSubmission() {
        doThrow(new IllegalStateException("queue unavailable")).when(jobs).enqueue(any());
        assertThatThrownBy(this::enqueue).isInstanceOf(IllegalStateException.class);
        assertThat(submissions.count()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM submission_review_job", Long.class)).isZero();
    }

    @Test
    void feedbackPersistenceFailureRollsBackCompletionAndAllowsRetry() {
        Long id = enqueue();
        ReviewJobClaim claim = jobs.claim(3, 300).orElseThrow();
        assertThatThrownBy(() -> completion.complete(claim, feedback("x".repeat(3001))))
                .isInstanceOf(RuntimeException.class);
        assertThat(jobStatus(id)).isEqualTo("RUNNING");
        assertThat(submission(id).getStatus()).isEqualTo(SubmissionStatus.PENDING);
        completion.fail(claim);
        assertThat(jobStatus(id)).isEqualTo("READY");
    }

    @Test
    void duplicateEnqueueDoesNotResetCompletedJob() {
        Long id = enqueue();
        ReviewJobClaim claim = jobs.claim(3, 300).orElseThrow();
        completion.complete(claim, feedback("Complete"));
        jobs.enqueue(id);
        assertThat(jobStatus(id)).isEqualTo("COMPLETED");
        assertThat(jobs.claim(3, 300)).isEmpty();
    }

    private Long enqueue() {
        SubmissionRequest request = new SubmissionRequest();
        ReflectionTestUtils.setField(request, "userId", userId);
        ReflectionTestUtils.setField(request, "submittedCode", "class Solution {}");
        return queued.createQueuedSubmission(problemId, request).id();
    }

    private Submission submission(Long id) {
        return submissions.findById(id).orElseThrow();
    }

    private String jobStatus(Long id) {
        return jdbc.queryForObject("SELECT status FROM submission_review_job WHERE submission_id = ?",
                String.class, id);
    }

    private void expireLease() {
        jdbc.update("UPDATE submission_review_job SET lease_until = CURRENT_TIMESTAMP - INTERVAL '1 second'");
    }

    private AiFeedbackResponse feedback(String summary) {
        return new AiFeedbackResponse(summary, "Correct", List.of(), List.of(),
                "O(n)", "O(1)", "Hint", "Improve");
    }
}
