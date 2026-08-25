package intercoach.repository;

import intercoach.model.AppUser;
import intercoach.model.Difficulty;
import intercoach.model.InterviewStatus;
import intercoach.model.MockInterviewSession;
import intercoach.model.Problem;
import intercoach.model.Submission;
import intercoach.model.SubmissionStatus;
import intercoach.model.TestCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
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
@ContextConfiguration(classes = RepositoryIntegrationTest.JpaTestApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
class RepositoryIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(PGVECTOR_IMAGE)
                    .withStartupTimeout(Duration.ofMinutes(2));

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private MockInterviewRepository mockInterviewRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            AppUser.class,
            MockInterviewSession.class,
            Problem.class,
            Submission.class,
            TestCase.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            AppUserRepository.class,
            MockInterviewRepository.class,
            ProblemRepository.class,
            SubmissionRepository.class,
            TestCaseRepository.class
    })
    static class JpaTestApplication {
    }

    @Test
    void appUserRepositoryFindsUsersByUsernameAndEmailCaseInsensitively() {
        AppUser user = user("coder", "coder@example.com");

        appUserRepository.saveAndFlush(user);

        assertThat(appUserRepository.existsByUsernameIgnoreCase("CODER"))
                .isTrue();
        assertThat(appUserRepository.existsByEmailIgnoreCase("CODER@EXAMPLE.COM"))
                .isTrue();
        assertThat(appUserRepository.findByUsernameIgnoreCase("CoDeR"))
                .contains(user);
        assertThat(appUserRepository.findByEmailIgnoreCase("Coder@Example.com"))
                .contains(user);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getRole()).isEqualTo("USER");
    }

    @Test
    void problemAndTestCaseRepositoriesFindRecordsByDifficultyAndProblem() {
        Problem easyProblem = problem("Two Sum", Difficulty.EASY, "Arrays");
        Problem hardProblem = problem("Median Stream", Difficulty.HARD, "Heaps");
        problemRepository.saveAllAndFlush(List.of(easyProblem, hardProblem));

        TestCase visibleCase = testCase(easyProblem, "[2,7,11,15], 9", "[0,1]", false);
        TestCase hiddenCase = testCase(easyProblem, "[3,3], 6", "[0,1]", true);
        testCaseRepository.saveAllAndFlush(List.of(visibleCase, hiddenCase));

        List<Problem> easyProblems = problemRepository.findByDifficulty(Difficulty.EASY);
        List<TestCase> problemCases =
                testCaseRepository.findByProblemId(easyProblem.getId());

        assertThat(easyProblems)
                .extracting(Problem::getTitle)
                .containsExactly("Two Sum");
        assertThat(problemCases)
                .extracting(TestCase::getExpectedOutput)
                .containsExactlyInAnyOrder("[0,1]", "[0,1]");
        assertThat(problemCases)
                .extracting(TestCase::isHidden)
                .containsExactlyInAnyOrder(false, true);
        assertThat(easyProblem.getCreatedAt()).isNotNull();
        assertThat(easyProblem.getUpdatedAt()).isNotNull();
    }

    @Test
    void submissionRepositoryFindsSubmissionsByProblemUserAndStatus() {
        AppUser user = appUserRepository.saveAndFlush(
                user("coder", "coder@example.com")
        );
        Problem problem = problemRepository.saveAndFlush(
                problem("Two Sum", Difficulty.EASY, "Arrays")
        );
        Submission reviewedSubmission = submission(
                user,
                problem,
                SubmissionStatus.REVIEWED
        );
        Submission failedSubmission = submission(
                user,
                problem,
                SubmissionStatus.FAILED
        );
        submissionRepository.saveAllAndFlush(
                List.of(reviewedSubmission, failedSubmission)
        );

        assertThat(submissionRepository.findByProblemId(problem.getId()))
                .hasSize(2);
        assertThat(submissionRepository.findByUserId(user.getId()))
                .hasSize(2);
        assertThat(submissionRepository.findByStatus(SubmissionStatus.REVIEWED))
                .extracting(Submission::getStatus)
                .containsExactly(SubmissionStatus.REVIEWED);
        assertThat(submissionRepository.findUserIdById(reviewedSubmission.getId()))
                .contains(user.getId());
        assertThat(reviewedSubmission.getCreatedAt()).isNotNull();
    }

    @Test
    void mockInterviewRepositoryOrdersSessionsByNewestFirst()
            throws InterruptedException {
        AppUser user = appUserRepository.saveAndFlush(
                user("coder", "coder@example.com")
        );
        Problem problem = problemRepository.saveAndFlush(
                problem("Two Sum", Difficulty.EASY, "Arrays")
        );
        MockInterviewSession firstSession = mockInterviewSession(user, problem);
        mockInterviewRepository.saveAndFlush(firstSession);

        // Persist a second session later so the repository sort is observable.
        Thread.sleep(10);
        MockInterviewSession secondSession = mockInterviewSession(user, problem);
        mockInterviewRepository.saveAndFlush(secondSession);

        List<MockInterviewSession> sessions =
                mockInterviewRepository.findByUserIdOrderByStartedAtDesc(
                        user.getId()
                );

        assertThat(sessions)
                .extracting(MockInterviewSession::getId)
                .containsExactly(secondSession.getId(), firstSession.getId());
        assertThat(mockInterviewRepository.findUserIdById(firstSession.getId()))
                .contains(user.getId());
        assertThat(firstSession.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(firstSession.getStartedAt()).isNotNull();
    }

    private AppUser user(String username, String email) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        return user;
    }

    private Problem problem(
            String title,
            Difficulty difficulty,
            String category
    ) {
        Problem problem = new Problem();
        problem.setTitle(title);
        problem.setDescription("Practice problem description.");
        problem.setDifficulty(difficulty);
        problem.setCategory(category);
        return problem;
    }

    private TestCase testCase(
            Problem problem,
            String input,
            String expectedOutput,
            boolean hidden
    ) {
        TestCase testCase = new TestCase();
        testCase.setProblem(problem);
        testCase.setInput(input);
        testCase.setExpectedOutput(expectedOutput);
        testCase.setHidden(hidden);
        return testCase;
    }

    private Submission submission(
            AppUser user,
            Problem problem,
            SubmissionStatus status
    ) {
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setSubmittedCode("class Solution {}");
        submission.setLanguage("Java");
        submission.setStatus(status);
        submission.setCorrectness(status == SubmissionStatus.REVIEWED
                ? "Correct"
                : "Incorrect");
        return submission;
    }

    private MockInterviewSession mockInterviewSession(
            AppUser user,
            Problem problem
    ) {
        MockInterviewSession session = new MockInterviewSession();
        session.setUser(user);
        session.setProblem(problem);
        session.setDurationMinutes(45);
        return session;
    }
}
