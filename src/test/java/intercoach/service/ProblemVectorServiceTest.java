package intercoach.service;

import intercoach.dto.ProblemVectorIndexResponse;
import intercoach.dto.ProblemVectorSearchRequest;
import intercoach.dto.ProblemVectorSearchResultResponse;
import intercoach.exception.ResourceNotFoundException;
import intercoach.model.Difficulty;
import intercoach.model.Problem;
import intercoach.repository.ProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProblemVectorServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private VectorStore vectorStore;

    private ProblemVectorService problemVectorService;

    @BeforeEach
    void setUp() {
        problemVectorService = new ProblemVectorService(
                problemRepository,
                vectorStore
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void indexAllProblemsWritesStableProblemDocuments() {
        Problem arrays = problem(1L, "Two Sum", Difficulty.EASY, "Arrays");
        Problem graphs = problem(2L, "Clone Graph", Difficulty.MEDIUM, "Graphs");

        given(problemRepository.findAll()).willReturn(List.of(arrays, graphs));

        ProblemVectorIndexResponse response =
                problemVectorService.indexAllProblems();

        ArgumentCaptor<List<Document>> documentsCaptor =
                ArgumentCaptor.forClass(List.class);

        then(vectorStore).should().delete(List.of("problem-1", "problem-2"));
        then(vectorStore).should().add(documentsCaptor.capture());

        List<Document> documents = documentsCaptor.getValue();

        assertThat(response.indexedProblems()).isEqualTo(2);
        assertThat(response.indexedDocuments()).isEqualTo(2);
        assertThat(documents)
                .extracting(Document::getId)
                .containsExactly("problem-1", "problem-2");
        assertThat(documents.getFirst().getText())
                .contains("Title: Two Sum")
                .contains("Category: Arrays");
        assertThat(documents.getFirst().getMetadata())
                .containsEntry("documentType", "problem")
                .containsEntry("problemId", 1L)
                .containsEntry("difficulty", "EASY");
    }

    @Test
    void indexProblemRejectsUnknownProblem() {
        given(problemRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> problemVectorService.indexProblem(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Problem not found with id: 99");
    }

    @Test
    void searchProblemsBuildsProblemScopedVectorSearch() {
        ProblemVectorSearchRequest request = searchRequest("graph traversal", 3);
        Document document = Document.builder()
                .id("problem-2")
                .text("Title: Clone Graph\nDescription: BFS and DFS practice")
                .metadata(Map.of(
                        "documentType", "problem",
                        "problemId", 2L,
                        "title", "Clone Graph",
                        "difficulty", "MEDIUM",
                        "category", "Graphs",
                        "tags", "dfs,bfs"
                ))
                .score(0.91)
                .build();

        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(document));

        List<ProblemVectorSearchResultResponse> responses =
                problemVectorService.searchProblems(request);

        ArgumentCaptor<SearchRequest> searchRequestCaptor =
                ArgumentCaptor.forClass(SearchRequest.class);

        then(vectorStore).should().similaritySearch(searchRequestCaptor.capture());

        SearchRequest searchRequest = searchRequestCaptor.getValue();

        assertThat(searchRequest.getQuery()).isEqualTo("graph traversal");
        assertThat(searchRequest.getTopK()).isEqualTo(3);
        assertThat(searchRequest.hasFilterExpression()).isTrue();
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().problemId()).isEqualTo(2L);
        assertThat(responses.getFirst().title()).isEqualTo("Clone Graph");
        assertThat(responses.getFirst().difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(responses.getFirst().score()).isEqualTo(0.91);
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
        problem.setDescription("Practice " + title);
        problem.setTags(category.toLowerCase());
        return problem;
    }

    private ProblemVectorSearchRequest searchRequest(String query, Integer topK) {
        ProblemVectorSearchRequest request = new ProblemVectorSearchRequest();

        ReflectionTestUtils.setField(request, "query", query);
        ReflectionTestUtils.setField(request, "topK", topK);

        return request;
    }
}
