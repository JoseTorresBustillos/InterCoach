package InterCoach.service;

import InterCoach.dto.ProgressBucketResponse;
import InterCoach.dto.UserDashboardResponse;
import InterCoach.exception.ResourceNotFoundException;
import InterCoach.model.AppUser;
import InterCoach.model.Difficulty;
import InterCoach.model.InterviewStatus;
import InterCoach.model.MockInterviewSession;
import InterCoach.model.Problem;
import InterCoach.model.Submission;
import InterCoach.model.SubmissionStatus;
import InterCoach.repository.AppUserRepository;
import InterCoach.repository.MockInterviewRepository;
import InterCoach.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserDashboardServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private MockInterviewRepository mockInterviewRepository;

    private UserDashboardService userDashboardService;

    @BeforeEach
    void setUp() {
        userDashboardService = new UserDashboardService(
                appUserRepository,
                submissionRepository,
                mockInterviewRepository
        );
    }

    @Test
    void getDashboardBuildsProgressSummaryFromUserActivity() {
        AppUser user = user();
        Problem arrays = problem(10L, "Two Sum", Difficulty.EASY, "Arrays");
        Problem graphs = problem(20L, "Clone Graph", Difficulty.MEDIUM, "Graphs");
        Submission reviewed = submission(
                100L,
                arrays,
                SubmissionStatus.REVIEWED,
                Instant.parse("2026-08-10T10:00:00Z")
        );
        Submission failed = submission(
                101L,
                arrays,
                SubmissionStatus.FAILED,
                Instant.parse("2026-08-11T10:00:00Z")
        );
        Submission pending = submission(
                102L,
                graphs,
                SubmissionStatus.PENDING,
                Instant.parse("2026-08-12T10:00:00Z")
        );
        MockInterviewSession completedInterview = interview(
                200L,
                arrays,
                InterviewStatus.COMPLETED,
                Instant.parse("2026-08-09T10:00:00Z")
        );
        MockInterviewSession activeInterview = interview(
                201L,
                graphs,
                InterviewStatus.IN_PROGRESS,
                Instant.parse("2026-08-12T11:00:00Z")
        );
        MockInterviewSession abandonedInterview = interview(
                202L,
                graphs,
                InterviewStatus.ABANDONED,
                Instant.parse("2026-08-08T10:00:00Z")
        );

        given(appUserRepository.findById(42L)).willReturn(Optional.of(user));
        given(submissionRepository.findByUserId(42L))
                .willReturn(List.of(reviewed, failed, pending));
        given(mockInterviewRepository.findByUserIdOrderByStartedAtDesc(42L))
                .willReturn(List.of(
                        activeInterview,
                        completedInterview,
                        abandonedInterview
                ));

        UserDashboardResponse response =
                userDashboardService.getDashboard(42L);

        assertThat(response.userId()).isEqualTo(42L);
        assertThat(response.totalSubmissions()).isEqualTo(3);
        assertThat(response.reviewedSubmissions()).isEqualTo(1);
        assertThat(response.failedSubmissions()).isEqualTo(1);
        assertThat(response.pendingSubmissions()).isEqualTo(1);
        assertThat(response.attemptedProblems()).isEqualTo(2);
        assertThat(response.reviewedProblems()).isEqualTo(1);
        assertThat(response.mockInterviewsStarted()).isEqualTo(3);
        assertThat(response.mockInterviewsCompleted()).isEqualTo(1);
        assertThat(response.mockInterviewsInProgress()).isEqualTo(1);
        assertThat(response.mockInterviewsAbandoned()).isEqualTo(1);
        assertThat(response.submissionsByDifficulty())
                .extracting(ProgressBucketResponse::name)
                .containsExactly("EASY", "MEDIUM");
        assertThat(response.submissionsByCategory())
                .extracting(ProgressBucketResponse::name)
                .containsExactly("Arrays", "Graphs");
        assertThat(response.recentSubmissions())
                .extracting(summary -> summary.submissionId())
                .containsExactly(102L, 101L, 100L);
        assertThat(response.recentMockInterviews())
                .extracting(summary -> summary.sessionId())
                .containsExactly(201L, 200L, 202L);
    }

    @Test
    void getDashboardReturnsEmptyProgressForNewUser() {
        AppUser user = user();

        given(appUserRepository.findById(42L)).willReturn(Optional.of(user));
        given(submissionRepository.findByUserId(42L)).willReturn(List.of());
        given(mockInterviewRepository.findByUserIdOrderByStartedAtDesc(42L))
                .willReturn(List.of());

        UserDashboardResponse response =
                userDashboardService.getDashboard(42L);

        assertThat(response.totalSubmissions()).isZero();
        assertThat(response.attemptedProblems()).isZero();
        assertThat(response.mockInterviewsStarted()).isZero();
        assertThat(response.submissionsByDifficulty()).isEmpty();
        assertThat(response.submissionsByCategory()).isEmpty();
        assertThat(response.recentSubmissions()).isEmpty();
        assertThat(response.recentMockInterviews()).isEmpty();
    }

    @Test
    void getDashboardRejectsUnknownUser() {
        given(appUserRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userDashboardService.getDashboard(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
    }

    private AppUser user() {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(
                user,
                "createdAt",
                Instant.parse("2026-08-01T10:00:00Z")
        );
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
}
