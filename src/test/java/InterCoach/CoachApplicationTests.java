package InterCoach;

// Basic Spring Boot context load test.

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import InterCoach.repository.AppUserRepository;
import InterCoach.repository.MockInterviewRepository;
import InterCoach.repository.ProblemRepository;
import InterCoach.repository.SubmissionRepository;
import InterCoach.repository.TestCaseRepository;

import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
@Import(CoachApplicationTests.AiTestConfiguration.class)
class CoachApplicationTests {

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
	void contextLoads() {
	}

	@TestConfiguration
	static class AiTestConfiguration {

		@Bean
		ChatClient.Builder chatClientBuilder() {
			return mock(ChatClient.Builder.class);
		}
	}
}
