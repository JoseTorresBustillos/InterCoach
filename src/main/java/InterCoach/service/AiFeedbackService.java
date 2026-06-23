package InterCoach.service;

import InterCoach.model.Problem;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiFeedbackService {

    private final ChatClient chatClient;

    public AiFeedbackService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String reviewSubmission(Problem problem, String submittedCode) {
        // Keep the prompt focused so the AI gives useful interview-style feedback.
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

                Respond using this exact structure:

                Correctness:
                Bugs or Issues:
                Time Complexity:
                Space Complexity:
                Edge Cases:
                Hint:
                Suggested Improvement:

                Do not rewrite the full solution unless necessary.
                Keep the feedback clear, concise, and interview-focused.
                """.formatted(
                problem.getTitle(),
                problem.getDescription(),
                problem.getDifficulty(),
                submittedCode
        );

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}