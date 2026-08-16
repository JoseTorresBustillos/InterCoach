package InterCoach.service;

import InterCoach.dto.StudyAssistantResponse;
import InterCoach.model.Problem;
import InterCoach.repository.ProblemRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudyAssistantService {

    private static final int RAG_TOP_K = 4;
    private static final String EMPTY_CONTEXT =
            "No stored problem context is available yet.";

    private final ProblemRepository problemRepository;
    private final ProblemVectorService problemVectorService;
    private final ChatClient chatClient;

    public StudyAssistantService(
            ProblemRepository problemRepository,
            ProblemVectorService problemVectorService,
            ChatClient.Builder chatClientBuilder
    ) {
        this.problemRepository = problemRepository;
        this.problemVectorService = problemVectorService;
        this.chatClient = chatClientBuilder.build();
    }

    public StudyAssistantResponse askQuestion(String question) {
        String context = buildContext(question);

        String prompt = """
                You are an interview prep study assistant.

                Use the retrieved problem context to answer the user's question.
                If the context is not enough, say what is missing instead of making things up.
                When a retrieved problem is relevant, mention its title before explaining.

                Retrieved Problem Context:
                %s

                User Question:
                %s

                Give a clear, beginner-friendly answer.
                Focus on problem-solving patterns, not just final answers.
                """.formatted(context, question);

        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return new StudyAssistantResponse(answer);
    }

    private String buildContext(String question) {
        List<Document> retrievedProblems = retrieveProblemDocuments(question);

        if (!retrievedProblems.isEmpty()) {
            return retrievedProblems.stream()
                    .map(this::formatRetrievedProblemContext)
                    .collect(Collectors.joining("\n---\n"));
        }

        return fallbackProblemContext();
    }

    private List<Document> retrieveProblemDocuments(String question) {
        try {
            return problemVectorService.searchProblemDocuments(
                    question,
                    RAG_TOP_K
            );
        } catch (RuntimeException exception) {
            // Retrieval is best-effort so an unindexed or unavailable vector store
            // does not block the assistant from using relational problem context.
            return List.of();
        }
    }

    private String fallbackProblemContext() {
        List<Problem> problems = problemRepository.findAll();

        if (problems.isEmpty()) {
            return EMPTY_CONTEXT;
        }

        return problems.stream()
                .map(this::formatProblemContext)
                .collect(Collectors.joining("\n---\n"));
    }

    private String formatRetrievedProblemContext(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        return """
                Title: %s
                Difficulty: %s
                Category: %s
                Tags: %s
                Retrieved Content:
                %s
                """.formatted(
                metadataValue(metadata, "title"),
                metadataValue(metadata, "difficulty"),
                metadataValue(metadata, "category"),
                metadataValue(metadata, "tags"),
                value(document.getText())
        );
    }

    private String formatProblemContext(Problem problem) {
        return """
                Title: %s
                Difficulty: %s
                Category: %s
                Tags: %s
                Description: %s
                Solution Explanation: %s
                """.formatted(
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getCategory(),
                problem.getTags(),
                problem.getDescription(),
                problem.getSolutionExplanation()
        );
    }

    private String metadataValue(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return "";
        }

        return value(metadata.get(key));
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
