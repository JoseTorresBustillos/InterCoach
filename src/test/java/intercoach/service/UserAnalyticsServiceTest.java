package intercoach.service;

import intercoach.dto.UserActivityTrendResponse;
import intercoach.dto.UserAnalyticsBucketResponse;
import intercoach.dto.UserAnalyticsResponse;
import intercoach.exception.ResourceNotFoundException;
import intercoach.model.AppUser;
import intercoach.model.Difficulty;
import intercoach.model.InterviewStatus;
import intercoach.model.MockInterviewSession;
import intercoach.model.Problem;
import intercoach.model.Submission;
import intercoach.model.SubmissionStatus;
import intercoach.repository.AppUserRepository;
import intercoach.repository.MockInterviewRepository;
import intercoach.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserAnalyticsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private MockInterviewRepository mockInterviewRepository;

    private UserAnalyticsService userAnalyticsService;

    @BeforeEach
    void setUp() {
        userAnalyticsService = new UserAnalyticsService(
                appUserRepository,
                submissionRepository,
                mockInterviewRepository,
                new SubmissionInsightService()
        );
    }

    @Test
    void getAnalyticsBuildsRatesBucketsAndActivityTrend() {
        AppUser user = user();
        Problem arrays = problem(10L, "Two Sum", Difficulty.EASY, "Arrays");
        Problem graphs = problem(20L, "Clone Graph", Difficulty.MEDIUM, "Graphs");
        Problem dynamicProgramming = problem(
                30L,
                "Coin Change",
                Difficulty.HARD,
                "Dynamic Programming"
        );
        Submission strongReviewed = submission(
                100L,
                arrays,
                SubmissionStatus.REVIEWED,
                Instant.parse("2026-08-18T10:00:00Z")
        );
        strongReviewed.setCorrectness("Correct and complete.");
        strongReviewed.setBugs("No bugs found.");
        Submission weakReviewed = submission(
                101L,
                arrays,
                SubmissionStatus.REVIEWED,
                Instant.parse("2026-08-18T11:00:00Z")
        );
        weakReviewed.setCorrectness("Partially correct.");
        weakReviewed.setBugs("Fails duplicate complements.");
        Submission failed = submission(
                102L,
                graphs,
                SubmissionStatus.FAILED,
                Instant.parse("2026-08-17T09:00:00Z")
        );
        Submission pending = submission(
                103L,
                dynamicProgramming,
                SubmissionStatus.PENDING,
                Instant.parse("2026-08-17T12:00:00Z")
        );
        MockInterviewSession activeInterview = interview(
                200L,
                graphs,
                InterviewStatus.IN_PROGRESS,
                Instant.parse("2026-08-17T13:00:00Z")
        );
        MockInterviewSession completedInterview = interview(
                201L,
                arrays,
                InterviewStatus.COMPLETED,
                Instant.parse("2026-08-18T13:00:00Z")
        );

        given(appUserRepository.findById(42L)).willReturn(Optional.of(user));
        given(submissionRepository.findByUserIdOrderByCreatedAtDesc(42L))
                .willReturn(List.of(
                        weakReviewed,
                        strongReviewed,
                        pending,
                        failed
                ));
        given(mockInterviewRepository.findByUserIdOrderByStartedAtDesc(42L))
                .willReturn(List.of(completedInterview, activeInterview));

        UserAnalyticsResponse response =
                userAnalyticsService.getAnalytics(42L);

        assertThat(response.userId()).isEqualTo(42L);
        assertThat(response.username()).isEqualTo("coder");
        assertThat(response.totalSubmissions()).isEqualTo(4);
        assertThat(response.reviewedSubmissions()).isEqualTo(2);
        assertThat(response.failedSubmissions()).isEqualTo(1);
        assertThat(response.pendingSubmissions()).isEqualTo(1);
        assertThat(response.weakSubmissions()).isEqualTo(2);
        assertThat(response.strongReviewedSubmissions()).isEqualTo(1);
        assertThat(response.distinctProblemsAttempted()).isEqualTo(3);
        assertThat(response.distinctProblemsReviewed()).isEqualTo(1);
        assertThat(response.totalMockInterviews()).isEqualTo(2);
        assertThat(response.completedMockInterviews()).isEqualTo(1);
        assertThat(response.reviewRate()).isEqualTo(50.0);
        assertThat(response.weakSubmissionRate()).isEqualTo(50.0);
        assertThat(response.strongestCategory()).isEqualTo("Arrays");
        assertThat(response.weakestCategory()).isEqualTo("Graphs");

        assertThat(response.difficultyBreakdown())
                .extracting(UserAnalyticsBucketResponse::name)
                .containsExactly("EASY", "MEDIUM", "HARD");
        assertThat(response.categoryBreakdown())
                .extracting(UserAnalyticsBucketResponse::name)
                .containsExactly("Arrays", "Dynamic Programming", "Graphs");

        UserAnalyticsBucketResponse arraysBucket =
                bucket(response.categoryBreakdown(), "Arrays");
        assertThat(arraysBucket.totalSubmissions()).isEqualTo(2);
        assertThat(arraysBucket.reviewedSubmissions()).isEqualTo(2);
        assertThat(arraysBucket.weakSubmissions()).isEqualTo(1);
        assertThat(arraysBucket.weakSubmissionRate()).isEqualTo(50.0);
        assertThat(arraysBucket.latestActivityAt())
                .isEqualTo(Instant.parse("2026-08-18T11:00:00Z"));

        UserAnalyticsBucketResponse graphsBucket =
                bucket(response.categoryBreakdown(), "Graphs");
        assertThat(graphsBucket.failedSubmissions()).isEqualTo(1);
        assertThat(graphsBucket.weakSubmissionRate()).isEqualTo(100.0);

        assertThat(response.activityTrend())
                .extracting(UserActivityTrendResponse::date)
                .containsExactly(
                        LocalDate.parse("2026-08-17"),
                        LocalDate.parse("2026-08-18")
                );
        assertThat(response.activityTrend().getFirst().submissions())
                .isEqualTo(2);
        assertThat(response.activityTrend().getFirst().failedSubmissions())
                .isEqualTo(1);
        assertThat(response.activityTrend().getFirst().mockInterviews())
                .isEqualTo(1);
        assertThat(response.activityTrend().getLast().reviewedSubmissions())
                .isEqualTo(2);
        assertThat(response.activityTrend().getLast().completedMockInterviews())
                .isEqualTo(1);
    }

    @Test
    void getAnalyticsReturnsEmptySummaryForNewUser() {
        AppUser user = user();

        given(appUserRepository.findById(42L)).willReturn(Optional.of(user));
        given(submissionRepository.findByUserIdOrderByCreatedAtDesc(42L))
                .willReturn(List.of());
        given(mockInterviewRepository.findByUserIdOrderByStartedAtDesc(42L))
                .willReturn(List.of());

        UserAnalyticsResponse response =
                userAnalyticsService.getAnalytics(42L);

        assertThat(response.totalSubmissions()).isZero();
        assertThat(response.weakSubmissions()).isZero();
        assertThat(response.totalMockInterviews()).isZero();
        assertThat(response.reviewRate()).isZero();
        assertThat(response.weakSubmissionRate()).isZero();
        assertThat(response.strongestCategory()).isNull();
        assertThat(response.weakestCategory()).isNull();
        assertThat(response.difficultyBreakdown()).isEmpty();
        assertThat(response.categoryBreakdown()).isEmpty();
        assertThat(response.activityTrend()).isEmpty();
    }

    @Test
    void getAnalyticsRejectsUnknownUser() {
        given(appUserRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userAnalyticsService.getAnalytics(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
    }

    private AppUser user() {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 42L);
        user.setUsername("coder");
        user.setEmail("coder@example.com");
        return user;
    }

    private Problem problem(
            Long id,
            String title,
            Difficulty difficulty,
            String category
    ) {
        Problem problem = new Problem();
        ReflectionTestUtils.setField(problem, "id", id);
        problem.setTitle(title);
        problem.setDifficulty(difficulty);
        problem.setCategory(category);
        return problem;
    }

    private Submission submission(
            Long id,
            Problem problem,
            SubmissionStatus status,
            Instant createdAt
    ) {
        Submission submission = new Submission();
        ReflectionTestUtils.setField(submission, "id", id);
        ReflectionTestUtils.setField(submission, "createdAt", createdAt);
        submission.setProblem(problem);
        submission.setStatus(status);
        submission.setSubmittedCode("class Solution {}");
        submission.setLanguage("Java");
        return submission;
    }

    private MockInterviewSession interview(
            Long id,
            Problem problem,
            InterviewStatus status,
            Instant startedAt
    ) {
        MockInterviewSession interview = new MockInterviewSession();
        ReflectionTestUtils.setField(interview, "id", id);
        ReflectionTestUtils.setField(interview, "startedAt", startedAt);
        interview.setProblem(problem);
        interview.setStatus(status);
        interview.setDurationMinutes(45);
        return interview;
    }

    private UserAnalyticsBucketResponse bucket(
            List<UserAnalyticsBucketResponse> buckets,
            String name
    ) {
        return buckets.stream()
                .filter(bucket -> bucket.name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
