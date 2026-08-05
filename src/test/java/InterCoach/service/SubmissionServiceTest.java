package InterCoach.service;

import InterCoach.dto.AiFeedbackResponse;
import InterCoach.dto.SubmissionRequest;
import InterCoach.dto.SubmissionResponse;
import InterCoach.model.AppUser;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AiFeedbackService aiFeedbackService;

    private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionService(
                submissionRepository,
                problemRepository,
                appUserRepository,
                aiFeedbackService
        );
    }

    @Test
    void createSubmissionStoresStructuredFeedbackWhenAiReviewSucceeds() {
        Problem problem = problem();
        AppUser user = user();
        SubmissionRequest request = submissionRequest();
        AiFeedbackResponse feedback = new AiFeedbackResponse(
                "Good direction.",
                "Partially correct.",
                List.of("Misses empty arrays."),
                List.of("Single-element input."),
                "O(n)",
                "O(1)",
                "Check boundaries.",
                "Add an early return."
        );

        given(problemRepository.findById(1L)).willReturn(Optional.of(problem));
        given(appUserRepository.findById(2L)).willReturn(Optional.of(user));
        given(aiFeedbackService.reviewSubmission(problem, "class Solution {}"))
                .willReturn(feedback);
        given(submissionRepository.save(any(Submission.class)))
                .willAnswer(invocation -> savedSubmission(invocation.getArgument(0)));

        SubmissionResponse response =
                submissionService.createSubmission(1L, request);

        assertThat(response.status()).isEqualTo(SubmissionStatus.REVIEWED);
        assertThat(response.aiFeedback()).isEqualTo("Good direction.");
        assertThat(response.feedbackSummary()).isEqualTo("Good direction.");
        assertThat(response.bugs()).isEqualTo("Misses empty arrays.");
        assertThat(response.edgeCases()).isEqualTo("Single-element input.");
        assertThat(response.hint()).isEqualTo("Check boundaries.");
        then(submissionRepository).should(times(2)).save(any(Submission.class));
    }

    @Test
    void createSubmissionMarksSubmissionFailedWhenAiReviewFails() {
        Problem problem = problem();
        AppUser user = user();
        SubmissionRequest request = submissionRequest();

        given(problemRepository.findById(1L)).willReturn(Optional.of(problem));
        given(appUserRepository.findById(2L)).willReturn(Optional.of(user));
        given(aiFeedbackService.reviewSubmission(problem, "class Solution {}"))
                .willThrow(new RuntimeException("provider unavailable"));
        given(submissionRepository.save(any(Submission.class)))
                .willAnswer(invocation -> savedSubmission(invocation.getArgument(0)));

        SubmissionResponse response =
                submissionService.createSubmission(1L, request);

        assertThat(response.status()).isEqualTo(SubmissionStatus.FAILED);
        assertThat(response.aiFeedback())
                .contains("AI feedback could not be generated")
                .contains("provider unavailable");
        then(submissionRepository).should(times(2)).save(any(Submission.class));
    }

    private Problem problem() {
        Problem problem = new Problem();
        ReflectionTestUtils.setField(problem, "id", 1L);
        problem.setTitle("Two Sum");
        problem.setDescription("Find two numbers that add to the target.");
        problem.setDifficulty(Difficulty.EASY);
        problem.setCategory("Arrays");
        return problem;
    }

    private AppUser user() {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 2L);
        user.setUsername("coder");
        user.setEmail("coder@example.com");
        return user;
    }

    private SubmissionRequest submissionRequest() {
        SubmissionRequest request = new SubmissionRequest();
        ReflectionTestUtils.setField(request, "userId", 2L);
        ReflectionTestUtils.setField(request, "submittedCode", "class Solution {}");
        ReflectionTestUtils.setField(request, "language", "Java");
        return request;
    }

    private Submission savedSubmission(Submission submission) {
        ReflectionTestUtils.setField(submission, "id", 10L);
        return submission;
    }
}
