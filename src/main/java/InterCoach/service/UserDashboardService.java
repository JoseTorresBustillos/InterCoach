package InterCoach.service;

import InterCoach.dto.ProgressBucketResponse;
import InterCoach.dto.RecentMockInterviewSummaryResponse;
import InterCoach.dto.RecentSubmissionSummaryResponse;
import InterCoach.dto.UserDashboardResponse;
import InterCoach.exception.ResourceNotFoundException;
import InterCoach.model.AppUser;
import InterCoach.model.Difficulty;
import InterCoach.model.InterviewStatus;
import InterCoach.model.MockInterviewSession;
import InterCoach.model.Problem;
import InterCoach.model.Submission;
import InterCoach.model.SubmissionStatus;
import InterCoach.repository.AppUserRepository;
import InterCoach.repository.MockInterviewRepository;
import InterCoach.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserDashboardService {

    private static final int RECENT_ACTIVITY_LIMIT = 5;

    private final AppUserRepository appUserRepository;
    private final SubmissionRepository submissionRepository;
    private final MockInterviewRepository mockInterviewRepository;

    public UserDashboardService(
            AppUserRepository appUserRepository,
            SubmissionRepository submissionRepository,
            MockInterviewRepository mockInterviewRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.submissionRepository = submissionRepository;
        this.mockInterviewRepository = mockInterviewRepository;
    }

    @Transactional(readOnly = true)
    public UserDashboardResponse getDashboard(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        )
                );

        List<Submission> submissions = submissionRepository.findByUserId(userId);
        List<MockInterviewSession> interviews =
                mockInterviewRepository.findByUserIdOrderByStartedAtDesc(userId);

        return new UserDashboardResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                submissions.size(),
                countSubmissions(submissions, SubmissionStatus.REVIEWED),
                countSubmissions(submissions, SubmissionStatus.FAILED),
                countSubmissions(submissions, SubmissionStatus.PENDING),
                countDistinctProblems(submissions),
                countDistinctReviewedProblems(submissions),
                interviews.size(),
                countInterviews(interviews, InterviewStatus.COMPLETED),
                countInterviews(interviews, InterviewStatus.IN_PROGRESS),
                countInterviews(interviews, InterviewStatus.ABANDONED),
                buildDifficultyBuckets(submissions),
                buildCategoryBuckets(submissions),
                buildRecentSubmissions(submissions),
                buildRecentInterviews(interviews)
        );
    }

    private List<ProgressBucketResponse> buildDifficultyBuckets(
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

    private List<ProgressBucketResponse> buildCategoryBuckets(
            List<Submission> submissions
    ) {
        Map<String, List<Submission>> submissionsByCategory =
                submissions.stream()
                        .collect(Collectors.groupingBy(this::categoryName));

        return submissionsByCategory.entrySet()
                .stream()
                .map(entry -> toBucket(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparing(ProgressBucketResponse::totalSubmissions)
                        .reversed()
                        .thenComparing(ProgressBucketResponse::name))
                .toList();
    }

    private List<RecentSubmissionSummaryResponse> buildRecentSubmissions(
            List<Submission> submissions
    ) {
        return submissions.stream()
                .sorted(this::compareSubmissionsNewestFirst)
                .limit(RECENT_ACTIVITY_LIMIT)
                .map(this::toRecentSubmission)
                .toList();
    }

    private List<RecentMockInterviewSummaryResponse> buildRecentInterviews(
            List<MockInterviewSession> interviews
    ) {
        return interviews.stream()
                .limit(RECENT_ACTIVITY_LIMIT)
                .map(this::toRecentInterview)
                .toList();
    }

    private ProgressBucketResponse toBucket(
            String name,
            List<Submission> submissions
    ) {
        return new ProgressBucketResponse(
                name,
                submissions.size(),
                countSubmissions(submissions, SubmissionStatus.REVIEWED),
                countSubmissions(submissions, SubmissionStatus.FAILED),
                countSubmissions(submissions, SubmissionStatus.PENDING)
        );
    }

    private RecentSubmissionSummaryResponse toRecentSubmission(
            Submission submission
    ) {
        Problem problem = submission.getProblem();

        return new RecentSubmissionSummaryResponse(
                submission.getId(),
                problem == null ? null : problem.getId(),
                problem == null ? null : problem.getTitle(),
                problem == null ? null : problem.getDifficulty(),
                problem == null ? null : problem.getCategory(),
                submission.getStatus(),
                submission.getCreatedAt()
        );
    }

    private RecentMockInterviewSummaryResponse toRecentInterview(
            MockInterviewSession interview
    ) {
        Problem problem = interview.getProblem();

        return new RecentMockInterviewSummaryResponse(
                interview.getId(),
                problem == null ? null : problem.getId(),
                problem == null ? null : problem.getTitle(),
                problem == null ? null : problem.getDifficulty(),
                interview.getStatus(),
                interview.getDurationMinutes(),
                interview.getStartedAt(),
                interview.getCompletedAt()
        );
    }

    private long countSubmissions(
            List<Submission> submissions,
            SubmissionStatus status
    ) {
        return submissions.stream()
                .filter(submission -> submission.getStatus() == status)
                .count();
    }

    private long countInterviews(
            List<MockInterviewSession> interviews,
            InterviewStatus status
    ) {
        return interviews.stream()
                .filter(interview -> interview.getStatus() == status)
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

    private boolean hasDifficulty(
            Submission submission,
            Difficulty difficulty
    ) {
        Problem problem = submission.getProblem();

        return problem != null && problem.getDifficulty() == difficulty;
    }

    private String categoryName(Submission submission) {
        Problem problem = submission.getProblem();

        if (problem == null
                || problem.getCategory() == null
                || problem.getCategory().isBlank()) {
            return "Uncategorized";
        }

        return problem.getCategory();
    }

    private int compareSubmissionsNewestFirst(
            Submission left,
            Submission right
    ) {
        Instant leftCreatedAt = left.getCreatedAt();
        Instant rightCreatedAt = right.getCreatedAt();

        if (leftCreatedAt == null && rightCreatedAt == null) {
            return 0;
        }

        if (leftCreatedAt == null) {
            return 1;
        }

        if (rightCreatedAt == null) {
            return -1;
        }

        return rightCreatedAt.compareTo(leftCreatedAt);
    }
}
