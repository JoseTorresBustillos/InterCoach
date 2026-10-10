package intercoach.submission.service;

import intercoach.problem.model.Problem;
import intercoach.submission.dto.AiFeedbackResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiFeedbackService {

    private final ChatClient chatClient;

    public AiFeedbackService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public AiFeedbackResponse reviewSubmission(Problem problem, String submittedCode) {
        return reviewSubmission(chatClient, problem, submittedCode);
    }

    static AiFeedbackResponse reviewSubmission(
            ChatClient client, Problem problem, String submittedCode
    ) {
        // Ask for strict JSON so the app can store feedback in separate fields.
        String prompt = """
                You are an experienced coding interview coach.

                Review this Java solution for the given interview problem.

                Problem Title:
                %s

                Problem Description:
                %s

                Difficulty:
                %s

                Submitted Code:
                %s

                Return only valid JSON with this exact shape:

                {
                  "summary": "...",
                  "correctness": "...",
                  "bugs": ["..."],
                  "edgeCases": ["..."],
                  "timeComplexity": "...",
                  "spaceComplexity": "...",
                  "hint": "...",
                  "suggestedImprovement": "..."
                }

                Do not include markdown.
                Do not wrap the JSON in code fences.
                Keep feedback clear, concise, and interview-focused.
                """.formatted(
                problem.getTitle(),
                problem.getDescription(),
                problem.getDifficulty(),
                submittedCode
        );

        return client.prompt()
                .user(prompt)
                .call()
                .entity(AiFeedbackResponse.class);
    }
}
