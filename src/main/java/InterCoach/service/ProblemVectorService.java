package InterCoach.service;

import InterCoach.dto.ProblemVectorIndexResponse;
import InterCoach.dto.ProblemVectorSearchRequest;
import InterCoach.dto.ProblemVectorSearchResultResponse;
import InterCoach.exception.ResourceNotFoundException;
import InterCoach.model.Difficulty;
import InterCoach.model.Problem;
import InterCoach.repository.ProblemRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProblemVectorService {

    private static final String DOCUMENT_TYPE = "problem";
    private static final String DOCUMENT_ID_PREFIX = "problem-";
    private static final int EXCERPT_LIMIT = 500;
    private static final int DEFAULT_TOP_K = 5;

    private final ProblemRepository problemRepository;
    private final VectorStore vectorStore;

    public ProblemVectorService(
            ProblemRepository problemRepository,
            VectorStore vectorStore
    ) {
        this.problemRepository = problemRepository;
        this.vectorStore = vectorStore;
    }

    @Transactional(readOnly = true)
    public ProblemVectorIndexResponse indexAllProblems() {
        List<Document> documents = problemRepository.findAll()
                .stream()
                .map(this::toDocument)
                .toList();

        replaceDocuments(documents);

        return new ProblemVectorIndexResponse(
                documents.size(),
                documents.size()
        );
    }

    @Transactional(readOnly = true)
    public ProblemVectorIndexResponse indexProblem(Long problemId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Problem not found with id: " + problemId
                        )
                );
        List<Document> documents = List.of(toDocument(problem));

        replaceDocuments(documents);

        return new ProblemVectorIndexResponse(1, documents.size());
    }

    public List<ProblemVectorSearchResultResponse> searchProblems(
            ProblemVectorSearchRequest request
    ) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(request.getQuery().trim())
                .topK(resolveTopK(request.getTopK()))
                .similarityThresholdAll()
                .filterExpression(problemDocumentFilter())
                .build();

        return vectorStore.similaritySearch(searchRequest)
                .stream()
                .map(this::toSearchResult)
                .toList();
    }

    private void replaceDocuments(List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }

        // Replacing by stable document ids keeps repeated indexing idempotent.
        vectorStore.delete(documents.stream()
                .map(Document::getId)
                .toList());
        vectorStore.add(documents);
    }

    private Document toDocument(Problem problem) {
        return Document.builder()
                .id(documentId(problem))
                .text(problemSearchText(problem))
                .metadata(metadata(problem))
                .build();
    }

    private String problemSearchText(Problem problem) {
        return """
                Title: %s
                Difficulty: %s
                Category: %s
                Tags: %s
                Description: %s
                Examples: %s
                Constraints: %s
                Starter Code: %s
                Solution Explanation: %s
                """.formatted(
                value(problem.getTitle()),
                problem.getDifficulty() == null
                        ? ""
                        : problem.getDifficulty().name(),
                value(problem.getCategory()),
                value(problem.getTags()),
                value(problem.getDescription()),
                value(problem.getExamples()),
                value(problem.getConstraints()),
                value(problem.getStarterCode()),
                value(problem.getSolutionExplanation())
        );
    }

    private Map<String, Object> metadata(Problem problem) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("documentType", DOCUMENT_TYPE);
        metadata.put("problemId", problem.getId());
        metadata.put("title", value(problem.getTitle()));
        metadata.put("difficulty", problem.getDifficulty() == null
                ? ""
                : problem.getDifficulty().name());
        metadata.put("category", value(problem.getCategory()));
        metadata.put("tags", value(problem.getTags()));

        return metadata;
    }

    private org.springframework.ai.vectorstore.filter.Filter.Expression problemDocumentFilter() {
        return new FilterExpressionBuilder()
                .eq("documentType", DOCUMENT_TYPE)
                .build();
    }

    private ProblemVectorSearchResultResponse toSearchResult(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        return new ProblemVectorSearchResultResponse(
                longMetadata(metadata.get("problemId")),
                stringMetadata(metadata.get("title")),
                difficultyMetadata(metadata.get("difficulty")),
                stringMetadata(metadata.get("category")),
                stringMetadata(metadata.get("tags")),
                document.getScore(),
                excerpt(document.getText())
        );
    }

    private int resolveTopK(Integer topK) {
        return topK == null ? DEFAULT_TOP_K : topK;
    }

    private String documentId(Problem problem) {
        return DOCUMENT_ID_PREFIX + problem.getId();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private Long longMetadata(Object value) {
        return switch (value) {
            case null -> null;
            case Number number -> number.longValue();
            case String text -> text.isBlank() ? null : Long.parseLong(text);
            default -> null;
        };
    }

    private String stringMetadata(Object value) {
        return value == null ? "" : value.toString();
    }

    private Difficulty difficultyMetadata(Object value) {
        String difficulty = stringMetadata(value);

        return difficulty.isBlank() ? null : Difficulty.valueOf(difficulty);
    }

    private String excerpt(String text) {
        if (text == null || text.length() <= EXCERPT_LIMIT) {
            return text;
        }

        return text.substring(0, EXCERPT_LIMIT);
    }
}
