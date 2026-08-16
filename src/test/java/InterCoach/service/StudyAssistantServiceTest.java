package InterCoach.service;

import InterCoach.dto.StudyAssistantResponse;
import InterCoach.model.Difficulty;
import InterCoach.model.Problem;
import InterCoach.repository.ProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StudyAssistantServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private ProblemVectorService problemVectorService;

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
        assertThat(promptCaptor.getValue())
                .contains("Retrieved Problem Context:")
                .contains("Title: Two Sum")
                .contains("Difficulty: EASY")
                .contains("Retrieved Content:")
                .contains("Hash map pair practice")
                .contains("User Question:\nHow do I solve pair sums?");
        verifyNoInteractions(problemRepository);
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
        assertThat(promptCaptor.getValue())
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
        Problem problem = new Problem();

        ReflectionTestUtils.setField(problem, "id", 3L);
        problem.setTitle("Climbing Stairs");
        problem.setDifficulty(Difficulty.EASY);
        problem.setCategory("Dynamic Programming");
        problem.setTags("dp");
        problem.setDescription("Count ways to climb stairs.");
        problem.setSolutionExplanation("Use previous two states.");

        return problem;
    }
}
