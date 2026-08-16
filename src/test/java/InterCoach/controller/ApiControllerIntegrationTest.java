package InterCoach.controller;

import InterCoach.model.AppUser;
import InterCoach.model.Difficulty;
import InterCoach.model.Problem;
import InterCoach.model.Submission;
import InterCoach.model.SubmissionStatus;
import InterCoach.model.TestCase;
import InterCoach.repository.AppUserRepository;
import InterCoach.repository.MockInterviewRepository;
import InterCoach.repository.ProblemRepository;
import InterCoach.repository.SubmissionRepository;
import InterCoach.repository.TestCaseRepository;
import InterCoach.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiControllerIntegrationTest.AiTestConfiguration.class)
class ApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private MockInterviewRepository mockInterviewRepository;

    @MockitoBean
    private ProblemRepository problemRepository;

    @MockitoBean
    private SubmissionRepository submissionRepository;

    @MockitoBean
    private TestCaseRepository testCaseRepository;

    @MockitoBean
    private VectorStore vectorStore;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("running"));
    }

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/problems"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required."))
                .andExpect(jsonPath("$.path").value("/api/problems"));
    }

    @Test
    void registerEndpointCreatesUserAndReturnsToken() throws Exception {
        given(appUserRepository.existsByUsernameIgnoreCase("coder"))
                .willReturn(false);
        given(appUserRepository.existsByEmailIgnoreCase("coder@example.com"))
                .willReturn(false);
        given(appUserRepository.save(any(AppUser.class)))
                .willAnswer(invocation -> savedUser(invocation.getArgument(0)));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "coder",
                                  "email": "coder@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("coder"))
                .andExpect(jsonPath("$.user.email").value("coder@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void loginEndpointAcceptsExistingCredentials() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "coder",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("coder"));
    }

    @Test
    void protectedEndpointAcceptsValidBearerToken() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(problemRepository.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/problems")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void dashboardEndpointAcceptsValidBearerToken() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(appUserRepository.findById(42L)).willReturn(Optional.of(user));
        given(submissionRepository.findByUserId(42L)).willReturn(List.of());
        given(mockInterviewRepository.findByUserIdOrderByStartedAtDesc(42L))
                .willReturn(List.of());

        mockMvc.perform(get("/api/users/42/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.username").value("coder"))
                .andExpect(jsonPath("$.totalSubmissions").value(0))
                .andExpect(jsonPath("$.recentSubmissions").isArray());
    }

    @Test
    void userRecommendationEndpointAcceptsValidBearerToken() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();
        Problem arrays = problem(1L, "Two Sum", Difficulty.EASY, "Arrays");
        Problem graphs = problem(2L, "Clone Graph", Difficulty.MEDIUM, "Graphs");

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(appUserRepository.existsById(42L)).willReturn(true);
        given(problemRepository.findAll()).willReturn(List.of(graphs, arrays));
        given(submissionRepository.findByUserId(42L))
                .willReturn(List.of(submission(arrays, SubmissionStatus.FAILED)));

        mockMvc.perform(get("/api/users/42/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].problemId").value(1))
                .andExpect(jsonPath("$[0].category").value("Arrays"))
                .andExpect(jsonPath("$[0].reason").value(
                        "Recommended because your submissions suggest this topic needs practice."
                ));
    }

    @Test
    void semanticSearchEndpointAcceptsValidBearerToken() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(Document.builder()
                        .id("problem-1")
                        .text("Title: Two Sum\nDescription: Array practice")
                        .metadata(Map.of(
                                "documentType", "problem",
                                "problemId", 1L,
                                "title", "Two Sum",
                                "difficulty", "EASY",
                                "category", "Arrays",
                                "tags", "hash-map"
                        ))
                        .score(0.93)
                        .build()));

        mockMvc.perform(post("/api/problems/semantic-search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "hash map array pair",
                                  "topK": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].problemId").value(1))
                .andExpect(jsonPath("$[0].title").value("Two Sum"))
                .andExpect(jsonPath("$[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$[0].score").value(0.93));
    }

    @Test
    void studyAssistantEndpointAcceptsValidBearerToken() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(Document.builder()
                        .id("problem-1")
                        .text("Title: Two Sum\nDescription: Hash map practice")
                        .metadata(Map.of(
                                "documentType", "problem",
                                "problemId", 1L,
                                "title", "Two Sum",
                                "difficulty", "EASY",
                                "category", "Arrays",
                                "tags", "hash-map"
                        ))
                        .score(0.93)
                        .build()));

        mockMvc.perform(post("/api/study-assistant/ask")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "How should I solve pair sum problems?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(
                        "Use retrieved problem context."
                ));
    }

    @Test
    void codeExecutionEndpointAcceptsValidBearerToken() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(problemRepository.existsById(1L)).willReturn(true);
        given(testCaseRepository.findByProblemId(1L))
                .willReturn(List.of(testCase(10L, "hello", "hello\n", false)));

        mockMvc.perform(post("/api/problems/1/run")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "language": "Java",
                                  "submittedCode": "public class Main { public static void main(String[] args) { java.util.Scanner scanner = new java.util.Scanner(System.in); System.out.println(scanner.nextLine()); } }"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemId").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.allPassed").value(true))
                .andExpect(jsonPath("$.totalTests").value(1))
                .andExpect(jsonPath("$.testCases[0].status").value("PASSED"));
    }

    private AppUser user(String username) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "createdAt", Instant.now());
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setRole("USER");
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

    private TestCase testCase(
            Long id,
            String input,
            String expectedOutput,
            boolean hidden
    ) {
        TestCase testCase = new TestCase();
        ReflectionTestUtils.setField(testCase, "id", id);
        testCase.setInput(input);
        testCase.setExpectedOutput(expectedOutput);
        testCase.setHidden(hidden);
        return testCase;
    }

    private AppUser savedUser(AppUser user) {
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "createdAt", Instant.now());
        return user;
    }

    @TestConfiguration
    static class AiTestConfiguration {

        @Bean
        ChatClient.Builder chatClientBuilder() {
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            ChatClient chatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec requestSpec =
                    mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec callResponseSpec =
                    mock(ChatClient.CallResponseSpec.class);

            given(builder.build()).willReturn(chatClient);
            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(any(String.class))).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(callResponseSpec);
            given(callResponseSpec.content())
                    .willReturn("Use retrieved problem context.");

            return builder;
        }
    }
}
