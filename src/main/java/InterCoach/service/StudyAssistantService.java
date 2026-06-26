package InterCoach.service;

import InterCoach.dto.StudyAssistantResponse;
import InterCoach.model.Problem;
import InterCoach.repository.ProblemRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudyAssistantService {

    private final ProblemRepository problemRepository;
    private final ChatClient chatClient;

    public StudyAssistantService(
            ProblemRepository problemRepository,
            ChatClient.Builder chatClientBuilder
    ) {
        this.problemRepository = problemRepository;
        this.chatClient = chatClientBuilder.build();
    }

    public StudyAssistantResponse askQuestion(String question) {
        List<Problem> problems = problemRepository.findAll();

        // For now, use stored problem data as context.
        // Later, pgvector will retrieve only the most relevant chunks.
        String context = problems.stream()
                .map(this::formatProblemContext)
                .toList()
                .toString();

        String prompt = """
                You are an interview prep study assistant.

                Use the provided problem bank context to answer the user's question.
                If the context is not enough, say what is missing instead of making things up.

                Problem Bank Context:
                %s

                User Question:
                %s

                Give a clear, beginner-friendly answer.
                Focus on problem-solving patterns, not just final answers.
                """.formatted(context, question);

        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return new StudyAssistantResponse(answer);
    }

    private String formatProblemContext(Problem problem) {
        return """
                Title: %s
                Difficulty: %s
                Category: %s
                Tags: %s
                Description: %s
                Solution Explanation: %s
                """.formatted(
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getCategory(),
                problem.getTags(),
                problem.getDescription(),
                problem.getSolutionExplanation()
        );
    }
}