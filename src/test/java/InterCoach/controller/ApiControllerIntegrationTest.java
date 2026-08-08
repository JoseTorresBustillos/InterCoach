package InterCoach.controller;

import InterCoach.model.AppUser;
import InterCoach.repository.AppUserRepository;
import InterCoach.repository.MockInterviewRepository;
import InterCoach.repository.ProblemRepository;
import InterCoach.repository.SubmissionRepository;
import InterCoach.repository.TestCaseRepository;
import InterCoach.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
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

    private AppUser user(String username) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "createdAt", Instant.now());
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setRole("USER");
        return user;
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
            return mock(ChatClient.Builder.class);
        }
    }
}
