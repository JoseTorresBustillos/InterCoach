package intercoach.submission.service;

import intercoach.problem.model.Problem;
import intercoach.submission.config.ReviewQueueProperties;
import intercoach.submission.dto.AiFeedbackResponse;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(name = "intercoach.review-queue.enabled", havingValue = "true")
public class QueuedAiFeedbackService implements AutoCloseable {

    private final HttpClient httpClient;
    private final ChatClient chatClient;

    public QueuedAiFeedbackService(
            OpenAiChatModel model,
            @Qualifier("openAiApi") OpenAiApi api,
            RestClient.Builder restClientBuilder,
            ReviewQueueProperties properties
    ) {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(httpClient);
        requests.setReadTimeout(Duration.ofSeconds(properties.getResponseTimeoutSeconds()));
        OpenAiApi boundedApi = api.mutate()
                .restClientBuilder(restClientBuilder.clone().requestFactory(requests))
                .build();
        // The durable queue owns retries; a job attempt makes one bounded provider request.
        OpenAiChatModel boundedModel = model.mutate()
                .openAiApi(boundedApi)
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).noBackoff().build())
                .build();
        chatClient = ChatClient.create(boundedModel);
    }

    public AiFeedbackResponse reviewSubmission(Problem problem, String submittedCode) {
        return AiFeedbackService.reviewSubmission(chatClient, problem, submittedCode);
    }

    @Override
    public void close() {
        httpClient.shutdownNow();
    }
}
