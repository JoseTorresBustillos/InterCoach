package intercoach.controller;

import intercoach.model.AppUser;
import intercoach.model.Difficulty;
import intercoach.model.Problem;
import intercoach.model.Submission;
import intercoach.model.SubmissionStatus;
import intercoach.model.TestCase;
import intercoach.config.CodeExecutionProperties;
import intercoach.exception.GlobalExceptionHandler;
import intercoach.repository.AppUserRepository;
import intercoach.repository.MockInterviewRepository;
import intercoach.repository.ProblemRepository;
import intercoach.repository.SubmissionRepository;
import intercoach.repository.TestCaseRepository;
import intercoach.security.InterCoachUserDetailsService;
import intercoach.security.JwtAuthenticationFilter;
import intercoach.security.JwtService;
import intercoach.security.SecurityConfig;
import intercoach.security.SecurityErrorHandler;
import intercoach.security.UserAccessService;
import intercoach.service.AiFeedbackService;
import intercoach.service.AppUserService;
import intercoach.service.AuthService;
import intercoach.service.CodeExecutionOperationsService;
import intercoach.service.CodeExecutionRunMonitor;
import intercoach.service.CodeExecutionService;
import intercoach.service.MockInterviewService;
import intercoach.service.ProblemService;
import intercoach.service.ProblemVectorService;
import intercoach.service.RecommendationService;
import intercoach.service.StudyAssistantService;
import intercoach.service.SubmissionInsightService;
import intercoach.service.SubmissionService;
import intercoach.service.TestCaseService;
import intercoach.service.UserAnalyticsService;
import intercoach.service.UserDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiControllerIntegrationTest.ControllerTestApplication.class)
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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(CodeExecutionProperties.class)
    @Import({
            AppUserController.class,
            AuthController.class,
            CodeExecutionController.class,
            CodeExecutionOperationsController.class,
            HealthController.class,
            MockInterviewController.class,
            ProblemController.class,
            ProblemVectorController.class,
            RecommendationController.class,
            StudyAssistantController.class,
            SubmissionController.class,
            TestCaseController.class,
            UserAnalyticsController.class,
            UserDashboardController.class,
            AiFeedbackService.class,
            AppUserService.class,
            AuthService.class,
            CodeExecutionOperationsService.class,
            CodeExecutionRunMonitor.class,
            CodeExecutionService.class,
            MockInterviewService.class,
            ProblemService.class,
            ProblemVectorService.class,
            RecommendationService.class,
            StudyAssistantService.class,
            SubmissionInsightService.class,
            SubmissionService.class,
            TestCaseService.class,
            UserAnalyticsService.class,
            UserDashboardService.class,
            GlobalExceptionHandler.class,
            InterCoachUserDetailsService.class,
            JwtAuthenticationFilter.class,
            JwtService.class,
            SecurityConfig.class,
            SecurityErrorHandler.class,
            UserAccessService.class
    })
    static class ControllerTestApplication {
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("running"));
    }

    @Test
    void actuatorHealthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuatorProbeEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void prometheusEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required."))
                .andExpect(jsonPath("$.path").value("/actuator/prometheus"));
    }

    @Test
    void prometheusEndpointAcceptsAuthenticatedRequest() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));

        mockMvc.perform(get("/actuator/prometheus")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("# HELP")));
    }

    @Test
    void frontendShellIsPublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void frontendAssetsIncludeLearningWorkflows() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"problems\"")))
                .andExpect(content().string(containsString("id=\"code-run-form\"")))
                .andExpect(content().string(containsString("id=\"submit-review\"")))
                .andExpect(content().string(containsString("id=\"authoring\"")))
                .andExpect(content().string(containsString(
                        "id=\"authoring-nav\" class=\"hidden\""
                )))
                .andExpect(content().string(containsString(
                        "id=\"authoring\" class=\"dashboard-grid reveal hidden\""
                )))
                .andExpect(content().string(containsString("id=\"problem-author-form\"")))
                .andExpect(content().string(containsString("id=\"test-case-form\"")))
                .andExpect(content().string(containsString("id=\"interviews\"")))
                .andExpect(content().string(containsString("id=\"start-interview\"")))
                .andExpect(content().string(containsString("id=\"admin-users\"")))
                .andExpect(content().string(containsString("id=\"admin-user-form\"")));

        mockMvc.perform(get("/assets/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/problems")))
                .andExpect(content().string(containsString("/submissions")))
                .andExpect(content().string(containsString("/run")))
                .andExpect(content().string(containsString("method: problemMethod")))
                .andExpect(content().string(containsString("method: \"DELETE\"")))
                .andExpect(content().string(containsString("/test-cases")))
                .andExpect(content().string(containsString("/mock-interviews")))
                .andExpect(content().string(containsString("data-interview-action=\"complete\"")))
                .andExpect(content().string(containsString("data-interview-action=\"abandon\"")))
                .andExpect(content().string(containsString("loadAdminUsers")))
                .andExpect(content().string(containsString("handleAdminUserCreate")))
                .andExpect(content().string(containsString("handleAdminUserStatusChange")))
                .andExpect(content().string(containsString("data-user-status-id")))
                .andExpect(content().string(containsString("isAdminSession()")))
                .andExpect(content().string(containsString("status.runtime?.totalRuns")))
                .andExpect(content().string(containsString("status.hostPolicy?.isolation")));
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
                .andExpect(jsonPath("$.user.role").value("USER"))
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
                .andExpect(jsonPath("$.user.username").value("coder"))
                .andExpect(jsonPath("$.user.role").value("USER"));
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
    void userScopedEndpointRejectsDifferentUserBearerToken() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/99/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied."))
                .andExpect(jsonPath("$.path").value("/api/users/99/dashboard"));
    }

    @Test
    void adminCanAccessDifferentUserDashboard() throws Exception {
        AppUser admin = user(1L, "admin", "ADMIN");
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        AppUser targetUser = user(99L, "learner", "USER");
        String token = jwtService.generateToken(admin).value();

        given(appUserRepository.findByUsernameIgnoreCase("admin"))
                .willReturn(Optional.of(admin));
        given(appUserRepository.findById(99L))
                .willReturn(Optional.of(targetUser));
        given(submissionRepository.findByUserId(99L)).willReturn(List.of());
        given(mockInterviewRepository.findByUserIdOrderByStartedAtDesc(99L))
                .willReturn(List.of());

        mockMvc.perform(get("/api/users/99/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(99))
                .andExpect(jsonPath("$.username").value("learner"));
    }

    @Test
    void executionOperationsEndpointRejectsNonAdmins() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));

        mockMvc.perform(get("/api/admin/execution/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied."))
                .andExpect(jsonPath("$.path").value("/api/admin/execution/status"));
    }

    @Test
    void executionOperationsEndpointAllowsAdmins() throws Exception {
        AppUser admin = user(1L, "admin", "ADMIN");
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(admin).value();

        given(appUserRepository.findByUsernameIgnoreCase("admin"))
                .willReturn(Optional.of(admin));

        mockMvc.perform(get("/api/admin/execution/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("LOCAL"))
                .andExpect(jsonPath("$.supportedLanguage").value("Java"))
                .andExpect(jsonPath("$.compileTimeoutSeconds").value(5))
                .andExpect(jsonPath("$.testTimeoutSeconds").value(2))
                .andExpect(jsonPath("$.visibleTestCasesOnly").value(true))
                .andExpect(jsonPath("$.temporaryWorkspacePerRun").value(true))
                .andExpect(jsonPath("$.childEnvironmentSanitized").value(true))
                .andExpect(jsonPath("$.hostPolicy.isolation")
                        .value("Local child process"))
                .andExpect(jsonPath("$.hostPolicy.localExecutionEnabled")
                        .value(true))
                .andExpect(jsonPath("$.hostPolicy.osLevelIsolation")
                        .value(false))
                .andExpect(jsonPath("$.runtime.totalRuns").value(0))
                .andExpect(jsonPath("$.runtime.successfulRuns").value(0))
                .andExpect(jsonPath("$.runtime.failedRuns").value(0))
                .andExpect(jsonPath("$.runtime.averageDurationMs").value(0))
                .andExpect(jsonPath("$.docker.image").value("eclipse-temurin:21-jdk"))
                .andExpect(jsonPath("$.docker.networkDisabled").value(true))
                .andExpect(jsonPath("$.docker.readOnlyRootFilesystem").value(true));
    }

    @Test
    void userManagementEndpointsRejectNonAdmins() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied."))
                .andExpect(jsonPath("$.path").value("/api/users"));

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "learner",
                                  "email": "learner@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied."))
                .andExpect(jsonPath("$.path").value("/api/users"));

        mockMvc.perform(patch("/api/users/99/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": false}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void userManagementEndpointsAllowAdmins() throws Exception {
        AppUser admin = user(1L, "admin", "ADMIN");
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        AppUser learner = user(42L, "learner", "USER");
        String token = jwtService.generateToken(admin).value();

        given(appUserRepository.findByUsernameIgnoreCase("admin"))
                .willReturn(Optional.of(admin));
        given(appUserRepository.findAll()).willReturn(List.of(admin, learner));
        given(appUserRepository.findById(42L)).willReturn(Optional.of(learner));
        given(appUserRepository.existsByUsernameIgnoreCase("newlearner"))
                .willReturn(false);
        given(appUserRepository.existsByEmailIgnoreCase("newlearner@example.com"))
                .willReturn(false);
        given(appUserRepository.save(any(AppUser.class)))
                .willAnswer(invocation -> savedUser(invocation.getArgument(0)));

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].username").value("learner"))
                .andExpect(jsonPath("$[1].role").value("USER"))
                .andExpect(jsonPath("$[1].active").value(true));

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newlearner",
                                  "email": "NewLearner@Example.COM",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newlearner"))
                .andExpect(jsonPath("$.email").value("newlearner@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(patch("/api/users/42/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/users/42/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void adminCannotSuspendCurrentAccount() throws Exception {
        AppUser admin = user(1L, "admin", "ADMIN");
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(admin).value();

        given(appUserRepository.findByUsernameIgnoreCase("admin"))
                .willReturn(Optional.of(admin));
        given(appUserRepository.findById(1L)).willReturn(Optional.of(admin));

        mockMvc.perform(patch("/api/users/1/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("You cannot suspend your own account."));
    }

    @Test
    void inactiveAccountBearerTokenIsRejectedImmediately() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();
        user.setActive(false);

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid or expired token."));
    }

    @Test
    void currentUserEndpointReturnsAuthenticatedProfile() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("coder"))
                .andExpect(jsonPath("$.email").value("coder@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void profileUpdateEndpointReturnsRefreshedToken() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(appUserRepository.existsByUsernameIgnoreCase("mentor"))
                .willReturn(false);
        given(appUserRepository.existsByEmailIgnoreCase("mentor@example.com"))
                .willReturn(false);
        given(appUserRepository.save(any(AppUser.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "mentor",
                                  "email": "mentor@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("mentor"))
                .andExpect(jsonPath("$.user.email").value("mentor@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void passwordChangeEndpointUpdatesAuthenticatedUserPassword() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));

        mockMvc.perform(patch("/api/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "password123",
                                  "newPassword": "newpassword123"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void analyticsEndpointAcceptsValidBearerToken() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(appUserRepository.findById(42L)).willReturn(Optional.of(user));
        given(submissionRepository.findByUserIdOrderByCreatedAtDesc(42L))
                .willReturn(List.of());
        given(mockInterviewRepository.findByUserIdOrderByStartedAtDesc(42L))
                .willReturn(List.of());

        mockMvc.perform(get("/api/users/42/analytics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.username").value("coder"))
                .andExpect(jsonPath("$.totalSubmissions").value(0))
                .andExpect(jsonPath("$.reviewRate").value(0.0))
                .andExpect(jsonPath("$.categoryBreakdown").isArray())
                .andExpect(jsonPath("$.activityTrend").isArray());
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
    void problemAuthoringEndpointsRejectNonAdmins() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));

        mockMvc.perform(post("/api/problems")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Two Sum",
                                  "description": "Find a pair.",
                                  "difficulty": "EASY"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));

        mockMvc.perform(put("/api/problems/7")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Pair Sum",
                                  "description": "Find a pair.",
                                  "difficulty": "EASY"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));

        mockMvc.perform(delete("/api/problems/7")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void problemAuthoringEndpointsAllowAdmins() throws Exception {
        AppUser admin = user(1L, "admin", "ADMIN");
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(admin).value();
        Problem existingProblem = problem(7L, "Two Sum", Difficulty.EASY, "Arrays");

        given(appUserRepository.findByUsernameIgnoreCase("admin"))
                .willReturn(Optional.of(admin));
        given(problemRepository.save(any(Problem.class)))
                .willAnswer(invocation -> savedProblem(invocation.getArgument(0), 7L));
        given(problemRepository.findById(7L)).willReturn(Optional.of(existingProblem));
        given(problemRepository.existsById(7L)).willReturn(true);

        mockMvc.perform(post("/api/problems")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Two Sum",
                                  "description": "Find two values that add to a target.",
                                  "difficulty": "EASY",
                                  "category": "Arrays"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Two Sum"))
                .andExpect(jsonPath("$.difficulty").value("EASY"))
                .andExpect(jsonPath("$.category").value("Arrays"));

        mockMvc.perform(put("/api/problems/7")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Pair Sum",
                                  "description": "Return indices for a matching pair.",
                                  "difficulty": "MEDIUM",
                                  "category": "Hash Maps"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Pair Sum"))
                .andExpect(jsonPath("$.difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.category").value("Hash Maps"));

        mockMvc.perform(delete("/api/problems/7")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void testCaseCreationRejectsNonAdminsAndReadsHideEvaluationCases() throws Exception {
        AppUser user = user("coder");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(user).value();
        Problem problem = problem(1L, "Two Sum", Difficulty.EASY, "Arrays");
        TestCase visibleCase = testCase(10L, "visible input", "visible output", false);
        TestCase hiddenCase = testCase(11L, "hidden input", "hidden output", true);
        visibleCase.setProblem(problem);
        hiddenCase.setProblem(problem);

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(problemRepository.existsById(1L)).willReturn(true);
        given(testCaseRepository.findByProblemId(1L))
                .willReturn(List.of(visibleCase, hiddenCase));

        mockMvc.perform(post("/api/problems/1/test-cases")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input": "nums=[2,7,11,15], target=9",
                                  "expectedOutput": "[0,1]",
                                  "hidden": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));

        mockMvc.perform(get("/api/problems/1/test-cases")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].hidden").value(false))
                .andExpect(jsonPath("$[?(@.hidden == true)]").isEmpty());
    }

    @Test
    void testCaseManagementEndpointsAllowAdmins() throws Exception {
        AppUser admin = user(1L, "admin", "ADMIN");
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        String token = jwtService.generateToken(admin).value();
        Problem problem = problem(1L, "Two Sum", Difficulty.EASY, "Arrays");
        TestCase testCase = savedTestCase(new TestCase(), 11L);
        testCase.setProblem(problem);
        testCase.setInput("nums=[2,7,11,15], target=9");
        testCase.setExpectedOutput("[0,1]");
        testCase.setHidden(true);

        given(appUserRepository.findByUsernameIgnoreCase("admin"))
                .willReturn(Optional.of(admin));
        given(problemRepository.findById(1L)).willReturn(Optional.of(problem));
        given(problemRepository.existsById(1L)).willReturn(true);
        given(testCaseRepository.save(any(TestCase.class)))
                .willAnswer(invocation -> savedTestCase(invocation.getArgument(0), 11L));
        given(testCaseRepository.findByProblemId(1L)).willReturn(List.of(testCase));

        mockMvc.perform(post("/api/problems/1/test-cases")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input": "nums=[2,7,11,15], target=9",
                                  "expectedOutput": "[0,1]",
                                  "hidden": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.problemId").value(1))
                .andExpect(jsonPath("$.input").value("nums=[2,7,11,15], target=9"))
                .andExpect(jsonPath("$.expectedOutput").value("[0,1]"))
                .andExpect(jsonPath("$.hidden").value(true));

        mockMvc.perform(get("/api/problems/1/test-cases")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].hidden").value(true));
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
        Problem problem = problem(1L, "Two Sum", Difficulty.EASY, "Arrays");
        Submission history = submission(problem, SubmissionStatus.FAILED);
        history.setBugs("Misses duplicate complements.");

        given(appUserRepository.findByUsernameIgnoreCase("coder"))
                .willReturn(Optional.of(user));
        given(submissionRepository.findByUserIdOrderByCreatedAtDesc(42L))
                .willReturn(List.of(history));
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
                ))
                .andExpect(jsonPath("$.citations[0].label").value("P1"))
                .andExpect(jsonPath("$.citations[0].type").value("PROBLEM"))
                .andExpect(jsonPath("$.citations[0].problemId").value(1))
                .andExpect(jsonPath("$.citations[0].title").value("Two Sum"))
                .andExpect(jsonPath("$.citations[1].label").value("H1"))
                .andExpect(jsonPath("$.citations[1].type").value("USER_HISTORY"))
                .andExpect(jsonPath("$.citations[1].submissionStatus").value("FAILED"));
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
        return user(42L, username, "USER");
    }

    private AppUser user(Long id, String username, String role) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", Instant.now());
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setRole(role);
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

    private Problem savedProblem(Problem problem, Long id) {
        ReflectionTestUtils.setField(problem, "id", id);
        ReflectionTestUtils.setField(problem, "createdAt", Instant.now());
        ReflectionTestUtils.setField(problem, "updatedAt", Instant.now());
        return problem;
    }

    private TestCase savedTestCase(TestCase testCase, Long id) {
        ReflectionTestUtils.setField(testCase, "id", id);
        ReflectionTestUtils.setField(testCase, "createdAt", Instant.now());
        return testCase;
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
