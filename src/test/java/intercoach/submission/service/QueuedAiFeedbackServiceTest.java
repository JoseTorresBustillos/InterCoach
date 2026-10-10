package intercoach.submission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import intercoach.problem.model.Difficulty;
import intercoach.problem.model.Problem;
import intercoach.submission.config.ReviewQueueProperties;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(10)
class QueuedAiFeedbackServiceTest {

    private final AtomicInteger requests = new AtomicInteger();
    private HttpServer server;
    private ExecutorService executor;
    private QueuedAiFeedbackService service;
    private OpenAiChatModel originalModel;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.start();
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("test-only-key").build();
        originalModel = OpenAiChatModel.builder().openAiApi(api).build();
        ReviewQueueProperties properties = new ReviewQueueProperties();
        properties.setConnectTimeoutSeconds(1);
        properties.setResponseTimeoutSeconds(1);
        service = new QueuedAiFeedbackService(originalModel, api, RestClient.builder(), properties);
    }

    @AfterEach
    void tearDown() {
        service.close();
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void decodesFeedbackWithoutChangingTheSharedModel() throws Exception {
        var options = originalModel.getDefaultOptions();
        String feedback = new ObjectMapper().writeValueAsString(Map.of("summary", "Looks correct"));
        String completion = new ObjectMapper().writeValueAsString(Map.of(
                "id", "test", "object", "chat.completion", "created", 1, "model", "test",
                "choices", List.of(Map.of("index", 0, "finish_reason", "stop",
                        "message", Map.of("role", "assistant", "content", feedback)))
        ));
        server.createContext("/v1/chat/completions", exchange -> respond(exchange, 200, completion));

        assertThat(service.reviewSubmission(problem(), "class Solution {}").summary())
                .isEqualTo("Looks correct");
        assertThat(requests).hasValue(1);
        assertThat(originalModel.getDefaultOptions()).isEqualTo(options);
    }

    @Test
    void transientFailureDoesNotPerformHiddenProviderRetries() {
        server.createContext("/v1/chat/completions", exchange ->
                respond(exchange, 503, "{\"error\":{\"message\":\"temporary\"}}"));
        assertThatThrownBy(() -> service.reviewSubmission(problem(), "class Solution {}"))
                .isInstanceOf(RuntimeException.class);
        assertThat(requests).hasValue(1);
    }

    @Test
    void boundsWaitingForResponseHeaders() {
        server.createContext("/v1/chat/completions", exchange -> stall(exchange, false));
        assertBoundedFailure();
    }

    @Test
    void boundsReadingAStalledResponseBody() {
        server.createContext("/v1/chat/completions", exchange -> stall(exchange, true));
        assertBoundedFailure();
    }

    private void assertBoundedFailure() {
        long start = System.nanoTime();
        assertThatThrownBy(() -> service.reviewSubmission(problem(), "class Solution {}"))
                .isInstanceOf(RuntimeException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
        assertThat(requests).hasValue(1);
    }

    private void stall(HttpExchange exchange, boolean sendHeaders) throws IOException {
        requests.incrementAndGet();
        try (exchange) {
            exchange.getRequestBody().readAllBytes();
            if (sendHeaders) {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write('{');
                exchange.getResponseBody().flush();
            }
            try {
                Thread.sleep(8000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        requests.incrementAndGet();
        try (exchange) {
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private Problem problem() {
        Problem problem = new Problem();
        problem.setTitle("Echo");
        problem.setDescription("Return the input");
        problem.setDifficulty(Difficulty.EASY);
        return problem;
    }
}
