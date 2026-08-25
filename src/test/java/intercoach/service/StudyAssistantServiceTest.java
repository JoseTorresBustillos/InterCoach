package intercoach.service;

import intercoach.dto.StudyAssistantCitationResponse;
import intercoach.dto.StudyAssistantResponse;
import intercoach.exception.ResourceNotFoundException;
import intercoach.model.Difficulty;
import intercoach.model.Problem;
import intercoach.model.Submission;
import intercoach.model.SubmissionStatus;
import intercoach.repository.AppUserRepository;
import intercoach.repository.ProblemRepository;
import intercoach.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StudyAssistantServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private ProblemVectorService problemVectorService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private StudyAssistantService studyAssistantService;

    @BeforeEach
    void setUp() {
        given(chatClientBuilder.build()).willReturn(chatClient);

        studyAssistantService = new StudyAssistantService(
                problemRepository,
                problemVectorService,
                appUserRepository,
                submissionRepository,
                chatClientBuilder
        );
    }

    @Test
    void askQuestionUsesRetrievedVectorContextWhenAvailable() {
        Document document = Document.builder()
                .id("problem-1")
                .text("Title: Two Sum\nDescription: Hash map pair practice")
                .metadata(Map.of(
                        "documentType", "problem",
                        "problemId", 1L,
                        "title", "Two Sum",
                        "difficulty", "EASY",
                        "category", "Arrays",
                        "tags", "hash-map"
                ))
                .score(0.94)
                .build();
        ArgumentCaptor<String> promptCaptor =
                assistantResponds("Use a hash map to store complements.");

        given(problemVectorService.searchProblemDocuments(
                "How do I solve pair sums?",
                4
        )).willReturn(List.of(document));

        StudyAssistantResponse response =
                studyAssistantService.askQuestion("How do I solve pair sums?");

        assertThat(response.answer())
                .isEqualTo("Use a hash map to store complements.");
        assertThat(response.citations())
                .extracting(StudyAssistantCitationResponse::label)
                .containsExactly("P1");
        assertThat(response.citations().getFirst().problemId()).isEqualTo(1L);
        assertThat(response.citations().getFirst().score()).isEqualTo(0.94);
        assertThat(response.citations().getFirst().excerpt())
                .contains("Hash map pair practice");
        assertThat(promptCaptor.getValue())
                .contains("Retrieved Problem Context:")
                .contains("[P1] Retrieved Problem")
                .contains("Title: Two Sum")
                .contains("Difficulty: EASY")
                .contains("Similarity Score: 0.94")
                .contains("Retrieved Content:")
                .contains("Hash map pair practice")
                .contains("User Question:\nHow do I solve pair sums?");
        verifyNoInteractions(
                problemRepository,
                appUserRepository,
                submissionRepository
        );
    }

    @Test
    void askQuestionIncludesUserHistoryWhenUserIdIsProvided() {
        Document document = Document.builder()
                .id("problem-1")
                .text("Title: Two Sum\nDescription: Hash map pair practice")
                .metadata(Map.of(
                        "documentType", "problem",
                        "problemId", 1L,
                        "title", "Two Sum",
                        "difficulty", "EASY",
                        "category", "Arrays",
                        "tags", "hash-map"
                ))
                .score(0.94)
                .build();
        Problem problem = problem(
                1L,
                "Two Sum",
                Difficulty.EASY,
                "Arrays"
        );
        Submission submission = submission(
                problem,
                SubmissionStatus.FAILED,
                Instant.parse("2026-08-17T12:00:00Z")
        );
        submission.setFeedbackSummary("The approach handles happy paths.");
        submission.setCorrectness("Fails when the complement appears twice.");
        submission.setBugs("Misses duplicate complements.");
        submission.setEdgeCases("Add duplicate-number tests.");
        submission.setSuggestedImprovement("Track complements in a hash map.");
        ArgumentCaptor<String> promptCaptor =
                assistantResponds("Practice complements [P1] and duplicates [H1].");

        given(appUserRepository.existsById(42L)).willReturn(true);
        given(problemVectorService.searchProblemDocuments(
                "What should I practice next?",
                4
        )).willReturn(List.of(document));
        given(submissionRepository.findByUserIdOrderByCreatedAtDesc(42L))
                .willReturn(List.of(submission));

        StudyAssistantResponse response = studyAssistantService.askQuestion(
                "What should I practice next?",
                42L
        );

        assertThat(response.answer())
                .isEqualTo("Practice complements [P1] and duplicates [H1].");
        assertThat(response.citations())
                .extracting(StudyAssistantCitationResponse::label)
                .containsExactly("P1", "H1");
        assertThat(response.citations().get(1).type())
                .isEqualTo("USER_HISTORY");
        assertThat(response.citations().get(1).submissionStatus())
                .isEqualTo(SubmissionStatus.FAILED);
        assertThat(response.citations().get(1).submittedAt())
                .isEqualTo(Instant.parse("2026-08-17T12:00:00Z"));
        assertThat(response.citations().get(1).excerpt())
                .contains("Misses duplicate complements.");
        assertThat(promptCaptor.getValue())
                .contains("Cite supporting context with source labels like [P1] or [H1].")
                .contains("User History Context:")
                .contains("[H1] Prior Submission")
                .contains("Problem: Two Sum")
                .contains("Status: FAILED")
                .contains("Bugs: Misses duplicate complements.")
                .contains("Submitted Code Excerpt:");
    }

    @Test
    void askQuestionRejectsUnknownUserBeforeRetrieval() {
        given(appUserRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() ->
                studyAssistantService.askQuestion("What should I study?", 99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");

        verifyNoInteractions(
                problemVectorService,
                problemRepository,
                submissionRepository
        );
    }

    @Test
    void askQuestionFallsBackToStoredProblemsWhenVectorSearchIsEmpty() {
        Problem problem = problem();
        ArgumentCaptor<String> promptCaptor =
                assistantResponds("Climbing Stairs is a dynamic programming pattern.");

        given(problemVectorService.searchProblemDocuments(
                "Explain dynamic programming",
                4
        )).willReturn(List.of());
        given(problemRepository.findAll()).willReturn(List.of(problem));

        StudyAssistantResponse response =
                studyAssistantService.askQuestion("Explain dynamic programming");

        assertThat(response.answer())
                .isEqualTo("Climbing Stairs is a dynamic programming pattern.");
        assertThat(response.citations())
                .extracting(StudyAssistantCitationResponse::label)
                .containsExactly("P1");
        assertThat(promptCaptor.getValue())
                .contains("[P1] Stored Problem")
                .contains("Title: Climbing Stairs")
                .contains("Solution Explanation: Use previous two states.");
    }

    @Test
    void askQuestionFallsBackToStoredProblemsWhenVectorSearchFails() {
        Problem problem = problem();
        ArgumentCaptor<String> promptCaptor =
                assistantResponds("Use the stored problem bank as context.");

        given(problemVectorService.searchProblemDocuments(
                "What should I practice?",
                4
        )).willThrow(new RuntimeException("vector store unavailable"));
        given(problemRepository.findAll()).willReturn(List.of(problem));

        StudyAssistantResponse response =
                studyAssistantService.askQuestion("What should I practice?");

        assertThat(response.answer())
                .isEqualTo("Use the stored problem bank as context.");
        assertThat(response.citations())
                .extracting(StudyAssistantCitationResponse::title)
                .containsExactly("Climbing Stairs");
        assertThat(promptCaptor.getValue())
                .contains("Title: Climbing Stairs")
                .contains("Retrieved Problem Context:");
    }

    private ArgumentCaptor<String> assistantResponds(String answer) {
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.user(promptCaptor.capture())).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
        given(callResponseSpec.content()).willReturn(answer);

        return promptCaptor;
    }

    private Problem problem() {
        return problem(
                3L,
                "Climbing Stairs",
                Difficulty.EASY,
                "Dynamic Programming"
        );
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
        problem.setTags("dp");
        problem.setDescription("Count ways to climb stairs.");
        problem.setSolutionExplanation("Use previous two states.");

        return problem;
    }

    private Submission submission(
            Problem problem,
            SubmissionStatus status,
            Instant createdAt
    ) {
        Submission submission = new Submission();

        ReflectionTestUtils.setField(submission, "id", 10L);
        ReflectionTestUtils.setField(submission, "createdAt", createdAt);
        submission.setProblem(problem);
        submission.setStatus(status);
        submission.setSubmittedCode("class Main {}");
        submission.setLanguage("Java");

        return submission;
    }
}
