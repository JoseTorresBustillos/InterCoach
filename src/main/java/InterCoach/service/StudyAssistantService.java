package InterCoach.service;

import InterCoach.dto.StudyAssistantCitationResponse;
import InterCoach.dto.StudyAssistantResponse;
import InterCoach.exception.ResourceNotFoundException;
import InterCoach.model.AppUser;
import InterCoach.model.Difficulty;
import InterCoach.model.Problem;
import InterCoach.model.Submission;
import InterCoach.repository.AppUserRepository;
import InterCoach.repository.ProblemRepository;
import InterCoach.repository.SubmissionRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StudyAssistantService {

    private static final int RAG_TOP_K = 4;
    private static final int USER_HISTORY_LIMIT = 5;
    private static final int CITATION_EXCERPT_LIMIT = 500;
    private static final String EMPTY_CONTEXT =
            "No stored problem context is available yet.";
    private static final String NO_USER_HISTORY =
            "No user history was requested.";

    private final ProblemRepository problemRepository;
    private final ProblemVectorService problemVectorService;
    private final AppUserRepository appUserRepository;
    private final SubmissionRepository submissionRepository;
    private final ChatClient chatClient;

    public StudyAssistantService(
            ProblemRepository problemRepository,
            ProblemVectorService problemVectorService,
            AppUserRepository appUserRepository,
            SubmissionRepository submissionRepository,
            ChatClient.Builder chatClientBuilder
    ) {
        this.problemRepository = problemRepository;
        this.problemVectorService = problemVectorService;
        this.appUserRepository = appUserRepository;
        this.submissionRepository = submissionRepository;
        this.chatClient = chatClientBuilder.build();
    }

    public StudyAssistantResponse askQuestion(String question) {
        return askQuestion(question, null);
    }

    public StudyAssistantResponse askQuestionForUser(
            String question,
            String username
    ) {
        AppUser user = appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username
                ));

        return buildAnswer(question, user.getId());
    }

    public StudyAssistantResponse askQuestion(String question, Long userId) {
        validateUserIfRequested(userId);

        return buildAnswer(question, userId);
    }

    private StudyAssistantResponse buildAnswer(String question, Long userId) {
        AssistantContext problemContext = buildProblemContext(question);
        AssistantContext userHistoryContext = buildUserHistoryContext(userId);

        String prompt = """
                You are an interview prep study assistant.

                Use the retrieved problem context to answer the user's question.
                Use the user's submission history when it helps personalize the advice.
                If the context is not enough, say what is missing instead of making things up.
                Cite supporting context with source labels like [P1] or [H1].
                When a retrieved problem is relevant, mention its title before explaining.

                Retrieved Problem Context:
                %s

                User History Context:
                %s

                User Question:
                %s

                Give a clear, beginner-friendly answer.
                Focus on problem-solving patterns, not just final answers.
                """.formatted(
                problemContext.text(),
                userHistoryContext.text(),
                question
        );

        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        List<StudyAssistantCitationResponse> citations = new ArrayList<>();
        citations.addAll(problemContext.citations());
        citations.addAll(userHistoryContext.citations());

        return new StudyAssistantResponse(answer, citations);
    }

    private void validateUserIfRequested(Long userId) {
        if (userId != null && !appUserRepository.existsById(userId)) {
            throw new ResourceNotFoundException(
                    "User not found with id: " + userId
            );
        }
    }

    private AssistantContext buildProblemContext(String question) {
        List<Document> retrievedProblems = retrieveProblemDocuments(question);

        if (!retrievedProblems.isEmpty()) {
            List<StudyAssistantCitationResponse> citations = new ArrayList<>();
            List<String> contextBlocks = new ArrayList<>();

            for (int index = 0; index < retrievedProblems.size(); index++) {
                String label = problemLabel(index);
                Document document = retrievedProblems.get(index);

                citations.add(toProblemCitation(
                        document,
                        label
                ));
                contextBlocks.add(formatRetrievedProblemContext(
                        document,
                        label
                ));
            }

            return new AssistantContext(
                    String.join("\n---\n", contextBlocks),
                    citations
            );
        }

        return fallbackProblemContext();
    }

    private AssistantContext buildUserHistoryContext(Long userId) {
        if (userId == null) {
            return new AssistantContext(NO_USER_HISTORY, List.of());
        }

        List<Submission> submissions = submissionRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(USER_HISTORY_LIMIT)
                .toList();

        if (submissions.isEmpty()) {
            return new AssistantContext(
                    "The user has no prior submissions yet.",
                    List.of()
            );
        }

        List<StudyAssistantCitationResponse> citations = new ArrayList<>();
        List<String> contextBlocks = new ArrayList<>();

        for (int index = 0; index < submissions.size(); index++) {
            String label = historyLabel(index);
            Submission submission = submissions.get(index);

            citations.add(toHistoryCitation(submission, label));
            contextBlocks.add(formatUserHistoryContext(submission, label));
        }

        return new AssistantContext(
                String.join("\n---\n", contextBlocks),
                citations
        );
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

    private AssistantContext fallbackProblemContext() {
        List<Problem> problems = problemRepository.findAll();

        if (problems.isEmpty()) {
            return new AssistantContext(EMPTY_CONTEXT, List.of());
        }

        List<StudyAssistantCitationResponse> citations = new ArrayList<>();
        List<String> contextBlocks = new ArrayList<>();

        for (int index = 0; index < problems.size(); index++) {
            String label = problemLabel(index);
            Problem problem = problems.get(index);

            citations.add(toFallbackProblemCitation(problem, label));
            contextBlocks.add(formatProblemContext(problem, label));
        }

        return new AssistantContext(
                String.join("\n---\n", contextBlocks),
                citations
        );
    }

    private String formatRetrievedProblemContext(
            Document document,
            String label
    ) {
        Map<String, Object> metadata = document.getMetadata();

        return """
                [%s] Retrieved Problem
                Title: %s
                Difficulty: %s
                Category: %s
                Tags: %s
                Similarity Score: %s
                Retrieved Content:
                %s
                """.formatted(
                label,
                metadataValue(metadata, "title"),
                metadataValue(metadata, "difficulty"),
                metadataValue(metadata, "category"),
                metadataValue(metadata, "tags"),
                value(document.getScore()),
                value(document.getText())
        );
    }

    private String formatProblemContext(Problem problem, String label) {
        return """
                [%s] Stored Problem
                Title: %s
                Difficulty: %s
                Category: %s
                Tags: %s
                Description: %s
                Solution Explanation: %s
                """.formatted(
                label,
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getCategory(),
                problem.getTags(),
                problem.getDescription(),
                problem.getSolutionExplanation()
        );
    }

    private String formatUserHistoryContext(
            Submission submission,
            String label
    ) {
        Problem problem = submission.getProblem();

        return """
                [%s] Prior Submission
                Problem: %s
                Difficulty: %s
                Category: %s
                Status: %s
                Submitted At: %s
                Feedback Summary: %s
                Correctness: %s
                Bugs: %s
                Edge Cases: %s
                Suggested Improvement: %s
                Submitted Code Excerpt:
                %s
                """.formatted(
                label,
                problemTitle(problem),
                difficultyName(problem),
                categoryName(problem),
                value(submission.getStatus()),
                value(submission.getCreatedAt()),
                value(submission.getFeedbackSummary()),
                value(submission.getCorrectness()),
                value(submission.getBugs()),
                value(submission.getEdgeCases()),
                value(submission.getSuggestedImprovement()),
                excerpt(value(submission.getSubmittedCode()))
        );
    }

    private StudyAssistantCitationResponse toProblemCitation(
            Document document,
            String label
    ) {
        Map<String, Object> metadata = document.getMetadata();

        return new StudyAssistantCitationResponse(
                label,
                "PROBLEM",
                longMetadata(metadata, "problemId"),
                metadataValue(metadata, "title"),
                difficultyMetadata(metadataValue(metadata, "difficulty")),
                metadataValue(metadata, "category"),
                metadataValue(metadata, "tags"),
                document.getScore(),
                null,
                null,
                excerpt(document.getText())
        );
    }

    private StudyAssistantCitationResponse toFallbackProblemCitation(
            Problem problem,
            String label
    ) {
        return new StudyAssistantCitationResponse(
                label,
                "PROBLEM",
                problem.getId(),
                value(problem.getTitle()),
                problem.getDifficulty(),
                value(problem.getCategory()),
                value(problem.getTags()),
                null,
                null,
                null,
                excerpt(problemCitationText(problem))
        );
    }

    private StudyAssistantCitationResponse toHistoryCitation(
            Submission submission,
            String label
    ) {
        Problem problem = submission.getProblem();

        return new StudyAssistantCitationResponse(
                label,
                "USER_HISTORY",
                problem == null ? null : problem.getId(),
                problemTitle(problem),
                problem == null ? null : problem.getDifficulty(),
                categoryName(problem),
                problem == null ? "" : value(problem.getTags()),
                null,
                submission.getStatus(),
                submission.getCreatedAt(),
                excerpt(historyCitationText(submission))
        );
    }

    private String problemCitationText(Problem problem) {
        return """
                Description: %s
                Solution Explanation: %s
                """.formatted(
                value(problem.getDescription()),
                value(problem.getSolutionExplanation())
        );
    }

    private String historyCitationText(Submission submission) {
        List<String> parts = new ArrayList<>();

        addIfPresent(parts, "Feedback", submission.getFeedbackSummary());
        addIfPresent(parts, "Correctness", submission.getCorrectness());
        addIfPresent(parts, "Bugs", submission.getBugs());
        addIfPresent(parts, "Edge Cases", submission.getEdgeCases());
        addIfPresent(
                parts,
                "Suggested Improvement",
                submission.getSuggestedImprovement()
        );

        if (parts.isEmpty()) {
            parts.add("Status: " + value(submission.getStatus()));
        }

        return String.join("\n", parts);
    }

    private void addIfPresent(
            List<String> parts,
            String label,
            String value
    ) {
        if (value != null && !value.isBlank()) {
            parts.add(label + ": " + value);
        }
    }

    private String problemLabel(int index) {
        return "P" + (index + 1);
    }

    private String historyLabel(int index) {
        return "H" + (index + 1);
    }

    private String problemTitle(Problem problem) {
        return problem == null ? "" : value(problem.getTitle());
    }

    private String categoryName(Problem problem) {
        return problem == null ? "" : value(problem.getCategory());
    }

    private String difficultyName(Problem problem) {
        return problem == null || problem.getDifficulty() == null
                ? ""
                : problem.getDifficulty().name();
    }

    private Long longMetadata(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return null;
        }

        Object value = metadata.get(key);

        return switch (value) {
            case null -> null;
            case Number number -> number.longValue();
            case String text -> text.isBlank() ? null : Long.parseLong(text);
            default -> null;
        };
    }

    private Difficulty difficultyMetadata(String difficulty) {
        if (difficulty.isBlank()) {
            return null;
        }

        return Difficulty.valueOf(difficulty);
    }

    private String excerpt(String text) {
        if (text == null || text.length() <= CITATION_EXCERPT_LIMIT) {
            return text;
        }

        return text.substring(0, CITATION_EXCERPT_LIMIT);
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

    private record AssistantContext(
            String text,
            List<StudyAssistantCitationResponse> citations
    ) {
    }
}
