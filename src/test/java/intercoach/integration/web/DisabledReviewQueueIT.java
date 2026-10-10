package intercoach.integration.web;

import intercoach.auth.security.JwtService;
import intercoach.interview.repository.MockInterviewRepository;
import intercoach.problem.repository.ProblemRepository;
import intercoach.problem.repository.TestCaseRepository;
import intercoach.submission.repository.SubmissionRepository;
import intercoach.submission.service.QueuedSubmissionService;
import intercoach.user.model.AppUser;
import intercoach.user.repository.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiControllerIT.ControllerTestApplication.class,
        properties = "intercoach.review-queue.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiControllerIT.AiTestConfiguration.class)
class DisabledReviewQueueIT {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @MockitoBean private AppUserRepository users;
    @MockitoBean private MockInterviewRepository interviews;
    @MockitoBean private ProblemRepository problems;
    @MockitoBean private SubmissionRepository submissions;
    @MockitoBean private TestCaseRepository testCases;
    @MockitoBean private VectorStore vectors;
    @MockitoBean private QueuedSubmissionService queued;

    @Test
    void disabledQueuedRouteIsNotFoundInsteadOfAnInternalError() throws Exception {
        mvc.perform(post("/api/problems/1/submissions/queued")
                        .header(HttpHeaders.AUTHORIZATION, authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":42,\"submittedCode\":\"class Solution {}\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resource not found."));
        verifyNoInteractions(queued, problems, submissions);
    }

    @Test
    void unmappedAuthenticatedRouteIsNotFound() throws Exception {
        mvc.perform(get("/api/does-not-exist")
                        .header(HttpHeaders.AUTHORIZATION, authorization()))
                .andExpect(status().isNotFound());
    }

    @Test
    void disabledRouteStillRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/problems/1/submissions/queued"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(queued);
    }

    private String authorization() {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 42L);
        user.setUsername("coder");
        user.setEmail("coder@example.test");
        user.setPasswordHash("unused-test-hash");
        given(users.findByUsernameIgnoreCase("coder")).willReturn(Optional.of(user));
        return "Bearer " + jwtService.generateToken(user).value();
    }
}
