package InterCoach.service;

import InterCoach.dto.RecommendationResponse;
import InterCoach.exception.ResourceNotFoundException;
import InterCoach.model.Problem;
import InterCoach.model.Submission;
import InterCoach.model.SubmissionStatus;
import InterCoach.repository.AppUserRepository;
import InterCoach.repository.ProblemRepository;
import InterCoach.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class RecommendationService {

    private static final int RECOMMENDATION_LIMIT = 5;

    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final AppUserRepository appUserRepository;

    public RecommendationService(
            ProblemRepository problemRepository,
            SubmissionRepository submissionRepository,
            AppUserRepository appUserRepository
    ) {
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations() {
        return buildRecommendations(
                problemRepository.findAll(),
                globalRecommendationHistory(),
                Set.of(),
                "Starter recommendation based on difficulty.",
                "Recommended because submission history suggests this topic "
                        + "needs practice."
        );
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationsForUser(Long userId) {
        if (!appUserRepository.existsById(userId)) {
            throw new ResourceNotFoundException(
                    "User not found with id: " + userId
            );
        }

        List<Submission> userHistory = submissionRepository.findByUserId(userId);

        return buildRecommendations(
                problemRepository.findAll(),
                userHistory,
                strongReviewedProblemIds(userHistory),
                "Starter recommendation based on your current progress.",
                "Recommended because your submissions suggest this topic "
                        + "needs practice."
        );
    }

    private List<RecommendationResponse> buildRecommendations(
            List<Problem> allProblems,
            List<Submission> history,
            Set<Long> excludedProblemIds,
            String starterReason,
            String weaknessReason
    ) {
        Map<String, Integer> weaknessScores = calculateWeaknessScores(history);
        List<Problem> candidates = candidateProblems(
                allProblems,
                excludedProblemIds
        );
        String reason = weaknessScores.isEmpty()
                ? starterReason
                : weaknessReason;

        return candidates.stream()
                .sorted(recommendationComparator(weaknessScores))
                .limit(RECOMMENDATION_LIMIT)
                .map(problem -> toResponse(problem, reason))
                .toList();
    }

    private List<Submission> globalRecommendationHistory() {
        List<Submission> history = new ArrayList<>();

        history.addAll(
                submissionRepository.findByStatus(SubmissionStatus.FAILED)
        );
        history.addAll(
                submissionRepository.findByStatus(SubmissionStatus.REVIEWED)
        );

        return history;
    }

    private Map<String, Integer> calculateWeaknessScores(
            List<Submission> submissions
    ) {
        Map<String, Integer> weaknessScores = new HashMap<>();

        for (Submission submission : submissions) {
            if (submission.getStatus() == SubmissionStatus.FAILED) {
                addWeaknessScore(
                        weaknessScores,
                        categoryFor(submission),
                        3
                );
            } else if (submission.getStatus() == SubmissionStatus.REVIEWED
                    && hasSignsOfWeakness(submission)) {
                addWeaknessScore(
                        weaknessScores,
                        categoryFor(submission),
                        2
                );
            }
        }

        return weaknessScores;
    }

    private List<Problem> candidateProblems(
            List<Problem> allProblems,
            Set<Long> excludedProblemIds
    ) {
        if (excludedProblemIds.isEmpty()) {
            return allProblems;
        }

        List<Problem> candidates = allProblems.stream()
                .filter(problem -> !excludedProblemIds.contains(problem.getId()))
                .toList();

        return candidates.isEmpty() ? allProblems : candidates;
    }

    private Set<Long> strongReviewedProblemIds(List<Submission> submissions) {
        Set<Long> weakProblemIds = submissions.stream()
                .filter(this::isWeakSubmission)
                .map(Submission::getProblem)
                .filter(Objects::nonNull)
                .map(Problem::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        return submissions.stream()
                .filter(this::isStrongReviewedSubmission)
                .map(Submission::getProblem)
                .filter(Objects::nonNull)
                .map(Problem::getId)
                .filter(Objects::nonNull)
                .filter(problemId -> !weakProblemIds.contains(problemId))
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean isWeakSubmission(Submission submission) {
        return submission.getStatus() == SubmissionStatus.FAILED
                || (submission.getStatus() == SubmissionStatus.REVIEWED
                        && hasSignsOfWeakness(submission));
    }

    private boolean isStrongReviewedSubmission(Submission submission) {
        return submission.getStatus() == SubmissionStatus.REVIEWED
                && !hasSignsOfWeakness(submission);
    }

    private boolean hasSignsOfWeakness(Submission submission) {
        String correctness = safeLower(submission.getCorrectness());
        String bugs = safeLower(submission.getBugs());

        return correctness.contains("incorrect")
                || correctness.contains("partial")
                || correctness.contains("partially")
                || hasBugSignals(bugs);
    }

    private boolean hasBugSignals(String bugs) {
        if (bugs.isBlank()
                || bugs.contains("no bug")
                || bugs.contains("no issue")
                || bugs.equals("none")) {
            return false;
        }

        return bugs.contains("bug")
                || bugs.contains("issue")
                || bugs.contains("fails");
    }

    private void addWeaknessScore(
            Map<String, Integer> weaknessScores,
            String category,
            int points
    ) {
        if (category == null || category.isBlank()) {
            return;
        }

        weaknessScores.merge(category, points, Integer::sum);
    }

    private Comparator<Problem> recommendationComparator(
            Map<String, Integer> weaknessScores
    ) {
        return Comparator
                .comparingInt((Problem problem) ->
                        scoreProblem(problem, weaknessScores)
                )
                .reversed()
                .thenComparingInt(this::difficultyRank)
                .thenComparing(
                        Problem::getTitle,
                        Comparator.nullsLast(String::compareToIgnoreCase)
                );
    }

    private int scoreProblem(
            Problem problem,
            Map<String, Integer> weaknessScores
    ) {
        int score = weaknessScores.getOrDefault(problem.getCategory(), 0);

        // Slightly prioritize easier problems so recommendations stay realistic.
        score += switch (difficultyRank(problem)) {
            case 0 -> 2;
            case 1 -> 1;
            default -> 0;
        };

        return score;
    }

    private int difficultyRank(Problem problem) {
        return problem.getDifficulty() == null
                ? Integer.MAX_VALUE
                : problem.getDifficulty().ordinal();
    }

    private String categoryFor(Submission submission) {
        Problem problem = submission.getProblem();

        return problem == null ? null : problem.getCategory();
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
