package InterCoach.service;

import InterCoach.dto.RecommendationResponse;
import InterCoach.model.Problem;
import InterCoach.model.Submission;
import InterCoach.model.SubmissionStatus;
import InterCoach.repository.ProblemRepository;
import InterCoach.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    public RecommendationService(
            ProblemRepository problemRepository,
            SubmissionRepository submissionRepository
    ) {
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations() {
        List<Problem> allProblems = problemRepository.findAll();

        Map<String, Integer> weaknessScores = calculateWeaknessScores();

        // If there is no history yet, recommend easier starter problems.
        if (weaknessScores.isEmpty()) {
            return allProblems.stream()
                    .sorted(Comparator.comparing(problem -> problem.getDifficulty().ordinal()))
                    .limit(5)
                    .map(problem -> toResponse(problem, "Starter recommendation based on difficulty."))
                    .toList();
        }

        return allProblems.stream()
                .sorted((a, b) -> Integer.compare(
                        scoreProblem(b, weaknessScores),
                        scoreProblem(a, weaknessScores)
                ))
                .limit(5)
                .map(problem -> toResponse(
                        problem,
                        "Recommended because your submission history suggests this topic needs practice."
                ))
                .toList();
    }

    private Map<String, Integer> calculateWeaknessScores() {
        Map<String, Integer> weaknessScores = new HashMap<>();

        List<Submission> failedSubmissions = submissionRepository.findByStatus(SubmissionStatus.FAILED);
        for (Submission submission : failedSubmissions) {
            addWeaknessScore(weaknessScores, submission.getProblem().getCategory(), 3);
        }

        List<Submission> reviewedSubmissions = submissionRepository.findByStatus(SubmissionStatus.REVIEWED);
        for (Submission submission : reviewedSubmissions) {
            if (hasSignsOfWeakness(submission)) {
                addWeaknessScore(weaknessScores, submission.getProblem().getCategory(), 2);
            }
        }

        return weaknessScores;
    }

    private boolean hasSignsOfWeakness(Submission submission) {
        String correctness = safeLower(submission.getCorrectness());
        String bugs = safeLower(submission.getBugs());

        return correctness.contains("incorrect")
                || correctness.contains("partial")
                || correctness.contains("partially")
                || bugs.contains("bug")
                || bugs.contains("issue")
                || bugs.contains("fails");
    }

    private void addWeaknessScore(Map<String, Integer> weaknessScores, String category, int points) {
        if (category == null || category.isBlank()) {
            return;
        }

        weaknessScores.merge(category, points, Integer::sum);
    }

    private int scoreProblem(Problem problem, Map<String, Integer> weaknessScores) {
        int score = weaknessScores.getOrDefault(problem.getCategory(), 0);

        // Slightly prioritize easier problems so recommendations stay realistic.
        score += switch (problem.getDifficulty()) {
            case EASY -> 2;
            case MEDIUM -> 1;
            case HARD -> 0;
        };

        return score;
    }

    private RecommendationResponse toResponse(Problem problem, String reason) {
        return new RecommendationResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getCategory(),
                problem.getTags(),
                reason
        );
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}