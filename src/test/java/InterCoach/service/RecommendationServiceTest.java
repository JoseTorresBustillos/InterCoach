package InterCoach.service;

import InterCoach.dto.RecommendationResponse;
import InterCoach.exception.ResourceNotFoundException;
import InterCoach.model.Difficulty;
import InterCoach.model.Problem;
import InterCoach.model.Submission;
import InterCoach.model.SubmissionStatus;
import InterCoach.repository.AppUserRepository;
import InterCoach.repository.ProblemRepository;
import InterCoach.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                problemRepository,
                submissionRepository,
                appUserRepository
        );
    }

    @Test
    void getRecommendationsForUserRanksByThatUsersWeaknesses() {
        Problem arrays = problem(1L, "Two Sum", Difficulty.EASY, "Arrays");
        Problem graphs = problem(2L, "Clone Graph", Difficulty.MEDIUM, "Graphs");
        Problem strings = problem(3L, "Longest Window", Difficulty.MEDIUM, "Strings");

        given(appUserRepository.existsById(42L)).willReturn(true);
        given(problemRepository.findAll())
                .willReturn(List.of(strings, graphs, arrays));
        given(submissionRepository.findByUserId(42L))
                .willReturn(List.of(
                        submission(arrays, SubmissionStatus.FAILED),
                        strongReviewedSubmission(graphs)
                ));

        List<RecommendationResponse> responses =
                recommendationService.getRecommendationsForUser(42L);

        assertThat(responses)
                .extracting(RecommendationResponse::category)
                .containsExactly("Arrays", "Strings");
        assertThat(responses.getFirst().reason())
                .isEqualTo("Recommended because your submissions suggest this topic needs practice.");
        then(submissionRepository).should(never())
                .findByStatus(any(SubmissionStatus.class));
    }

    @Test
    void getRecommendationsForUserFallsBackToStarterProblemsWhenHistoryHasNoWeaknesses() {
        Problem arrays = problem(1L, "Two Sum", Difficulty.EASY, "Arrays");
        Problem graphs = problem(2L, "Clone Graph", Difficulty.MEDIUM, "Graphs");

        given(appUserRepository.existsById(42L)).willReturn(true);
        given(problemRepository.findAll()).willReturn(List.of(graphs, arrays));
        given(submissionRepository.findByUserId(42L))
                .willReturn(List.of(strongReviewedSubmission(arrays)));

        List<RecommendationResponse> responses =
                recommendationService.getRecommendationsForUser(42L);

        assertThat(responses)
                .extracting(RecommendationResponse::title)
                .containsExactly("Clone Graph");
        assertThat(responses.getFirst().reason())
                .isEqualTo("Starter recommendation based on your current progress.");
    }

    @Test
    void getRecommendationsForUserRejectsUnknownUser() {
        given(appUserRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() ->
                recommendationService.getRecommendationsForUser(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
    }

    @Test
    void getRecommendationsKeepsGlobalRankingBehavior() {
        Problem arrays = problem(1L, "Two Sum", Difficulty.EASY, "Arrays");
        Problem graphs = problem(2L, "Clone Graph", Difficulty.MEDIUM, "Graphs");

        given(problemRepository.findAll()).willReturn(List.of(arrays, graphs));
        given(submissionRepository.findByStatus(SubmissionStatus.FAILED))
                .willReturn(List.of(submission(graphs, SubmissionStatus.FAILED)));
        given(submissionRepository.findByStatus(SubmissionStatus.REVIEWED))
                .willReturn(List.of());

        List<RecommendationResponse> responses =
                recommendationService.getRecommendations();

        assertThat(responses)
                .extracting(RecommendationResponse::category)
                .containsExactly("Graphs", "Arrays");
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
        problem.setTags(category.toLowerCase());
        return problem;
    }

    private Submission submission(
            Problem problem,
            SubmissionStatus status
    ) {
        Submission submission = new Submission();
        submission.setProblem(problem);
        submission.setStatus(status);
        submission.setSubmittedCode("class Solution {}");
        submission.setLanguage("Java");
        return submission;
    }

    private Submission strongReviewedSubmission(Problem problem) {
        Submission submission = submission(problem, SubmissionStatus.REVIEWED);
        submission.setCorrectness("Correct and complete.");
        submission.setBugs("No bugs found.");
        return submission;
    }
}
