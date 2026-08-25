package intercoach.service;

import intercoach.dto.UserActivityTrendResponse;
import intercoach.dto.UserAnalyticsBucketResponse;
import intercoach.dto.UserAnalyticsResponse;
import intercoach.exception.ResourceNotFoundException;
import intercoach.model.AppUser;
import intercoach.model.Difficulty;
import intercoach.model.InterviewStatus;
import intercoach.model.MockInterviewSession;
import intercoach.model.Problem;
import intercoach.model.Submission;
import intercoach.model.SubmissionStatus;
import intercoach.repository.AppUserRepository;
import intercoach.repository.MockInterviewRepository;
import intercoach.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserAnalyticsService {

    private final AppUserRepository appUserRepository;
    private final SubmissionRepository submissionRepository;
    private final MockInterviewRepository mockInterviewRepository;
    private final SubmissionInsightService submissionInsightService;

    public UserAnalyticsService(
            AppUserRepository appUserRepository,
            SubmissionRepository submissionRepository,
            MockInterviewRepository mockInterviewRepository,
            SubmissionInsightService submissionInsightService
    ) {
        this.appUserRepository = appUserRepository;
        this.submissionRepository = submissionRepository;
        this.mockInterviewRepository = mockInterviewRepository;
        this.submissionInsightService = submissionInsightService;
    }

    @Transactional(readOnly = true)
    public UserAnalyticsResponse getAnalytics(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        )
                );
        List<Submission> submissions =
                submissionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<MockInterviewSession> interviews =
                mockInterviewRepository.findByUserIdOrderByStartedAtDesc(userId);
        List<UserAnalyticsBucketResponse> categoryBreakdown =
                buildCategoryBreakdown(submissions);

        long reviewedSubmissions =
                countSubmissions(submissions, SubmissionStatus.REVIEWED);
        long failedSubmissions =
                countSubmissions(submissions, SubmissionStatus.FAILED);
        long pendingSubmissions =
                countSubmissions(submissions, SubmissionStatus.PENDING);
        long weakSubmissions = countWeakSubmissions(submissions);
        long strongReviewedSubmissions =
                countStrongReviewedSubmissions(submissions);

        return new UserAnalyticsResponse(
                user.getId(),
                user.getUsername(),
                submissions.size(),
                reviewedSubmissions,
                failedSubmissions,
                pendingSubmissions,
                weakSubmissions,
                strongReviewedSubmissions,
                countDistinctProblems(submissions),
                countDistinctReviewedProblems(submissions),
                interviews.size(),
                countCompletedInterviews(interviews),
                percentage(reviewedSubmissions, submissions.size()),
                percentage(weakSubmissions, submissions.size()),
                strongestCategory(categoryBreakdown),
                weakestCategory(categoryBreakdown),
                buildDifficultyBreakdown(submissions),
                categoryBreakdown,
                buildActivityTrend(submissions, interviews)
        );
    }

    private List<UserAnalyticsBucketResponse> buildDifficultyBreakdown(
            List<Submission> submissions
    ) {
        return Arrays.stream(Difficulty.values())
                .map(difficulty -> toBucket(
                        difficulty.name(),
                        submissions.stream()
                                .filter(submission ->
                                        hasDifficulty(submission, difficulty)
                                )
                                .toList()
                ))
                .filter(bucket -> bucket.totalSubmissions() > 0)
                .toList();
    }

    private List<UserAnalyticsBucketResponse> buildCategoryBreakdown(
            List<Submission> submissions
    ) {
        Map<String, List<Submission>> submissionsByCategory =
                submissions.stream()
                        .collect(Collectors.groupingBy(this::categoryName));

        return submissionsByCategory.entrySet()
                .stream()
                .map(entry -> toBucket(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparing(UserAnalyticsBucketResponse::totalSubmissions)
                        .reversed()
                        .thenComparing(UserAnalyticsBucketResponse::name))
                .toList();
    }

    private List<UserActivityTrendResponse> buildActivityTrend(
            List<Submission> submissions,
            List<MockInterviewSession> interviews
    ) {
        Map<LocalDate, ActivityCounts> activityCounts = new HashMap<>();

        for (Submission submission : submissions) {
            if (submission.getCreatedAt() == null) {
                continue;
            }

            ActivityCounts counts = activityCounts.computeIfAbsent(
                    toDate(submission.getCreatedAt()),
                    date -> new ActivityCounts()
            );
            counts.submissions++;

            if (submission.getStatus() == SubmissionStatus.REVIEWED) {
                counts.reviewedSubmissions++;
            } else if (submission.getStatus() == SubmissionStatus.FAILED) {
                counts.failedSubmissions++;
            }
        }

        for (MockInterviewSession interview : interviews) {
            if (interview.getStartedAt() == null) {
                continue;
            }

            ActivityCounts counts = activityCounts.computeIfAbsent(
                    toDate(interview.getStartedAt()),
                    date -> new ActivityCounts()
            );
            counts.mockInterviews++;

            if (interview.getStatus() == InterviewStatus.COMPLETED) {
                counts.completedMockInterviews++;
            }
        }

        return activityCounts.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new UserActivityTrendResponse(
                        entry.getKey(),
                        entry.getValue().submissions,
                        entry.getValue().reviewedSubmissions,
                        entry.getValue().failedSubmissions,
                        entry.getValue().mockInterviews,
                        entry.getValue().completedMockInterviews
                ))
                .toList();
    }

    private UserAnalyticsBucketResponse toBucket(
            String name,
            List<Submission> submissions
    ) {
        long weakSubmissions = countWeakSubmissions(submissions);

        return new UserAnalyticsBucketResponse(
                name,
                submissions.size(),
                countSubmissions(submissions, SubmissionStatus.REVIEWED),
                countSubmissions(submissions, SubmissionStatus.FAILED),
                weakSubmissions,
                percentage(weakSubmissions, submissions.size()),
                latestSubmissionAt(submissions)
        );
    }

    private String strongestCategory(
            List<UserAnalyticsBucketResponse> categoryBreakdown
    ) {
        return categoryBreakdown.stream()
                .filter(bucket -> bucket.reviewedSubmissions() > 0)
                .sorted(Comparator
                        .comparingDouble(
                                UserAnalyticsBucketResponse::weakSubmissionRate
                        )
                        .thenComparing(
                                UserAnalyticsBucketResponse::reviewedSubmissions,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(UserAnalyticsBucketResponse::name))
                .map(UserAnalyticsBucketResponse::name)
                .findFirst()
                .orElse(null);
    }

    private String weakestCategory(
            List<UserAnalyticsBucketResponse> categoryBreakdown
    ) {
        return categoryBreakdown.stream()
                .filter(bucket -> bucket.weakSubmissions() > 0)
                .sorted(Comparator
                        .comparingDouble(
                                UserAnalyticsBucketResponse::weakSubmissionRate
                        )
                        .reversed()
                        .thenComparing(
                                UserAnalyticsBucketResponse::weakSubmissions,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(UserAnalyticsBucketResponse::name))
                .map(UserAnalyticsBucketResponse::name)
                .findFirst()
                .orElse(null);
    }

    private long countSubmissions(
            List<Submission> submissions,
            SubmissionStatus status
    ) {
        return submissions.stream()
                .filter(submission -> submission.getStatus() == status)
                .count();
    }

    private long countWeakSubmissions(List<Submission> submissions) {
        return submissions.stream()
                .filter(submissionInsightService::isWeakSubmission)
                .count();
    }

    private long countStrongReviewedSubmissions(List<Submission> submissions) {
        return submissions.stream()
                .filter(submissionInsightService::isStrongReviewedSubmission)
                .count();
    }

    private long countDistinctProblems(List<Submission> submissions) {
        return submissions.stream()
                .map(Submission::getProblem)
                .filter(Objects::nonNull)
                .map(Problem::getId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private long countDistinctReviewedProblems(List<Submission> submissions) {
        return submissions.stream()
                .filter(submission ->
                        submission.getStatus() == SubmissionStatus.REVIEWED
                )
                .map(Submission::getProblem)
                .filter(Objects::nonNull)
                .map(Problem::getId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private long countCompletedInterviews(
            List<MockInterviewSession> interviews
    ) {
        return interviews.stream()
                .filter(interview ->
                        interview.getStatus() == InterviewStatus.COMPLETED
                )
                .count();
    }

    private boolean hasDifficulty(
            Submission submission,
            Difficulty difficulty
    ) {
        Problem problem = submission.getProblem();

        return problem != null && problem.getDifficulty() == difficulty;
    }

    private String categoryName(Submission submission) {
        String category = submissionInsightService.categoryFor(submission);

        if (category == null || category.isBlank()) {
            return "Uncategorized";
        }

        return category;
    }

    private Instant latestSubmissionAt(List<Submission> submissions) {
        return submissions.stream()
                .map(Submission::getCreatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private double percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }

        return Math.round((numerator * 10000.0) / denominator) / 100.0;
    }

    private LocalDate toDate(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static class ActivityCounts {

        private long submissions;
        private long reviewedSubmissions;
        private long failedSubmissions;
        private long mockInterviews;
        private long completedMockInterviews;
    }
}
