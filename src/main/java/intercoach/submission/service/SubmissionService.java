package intercoach.submission.service;

import intercoach.problem.model.Problem;
import intercoach.problem.repository.ProblemRepository;
import intercoach.shared.exception.ResourceNotFoundException;
import intercoach.submission.dto.AiFeedbackResponse;
import intercoach.submission.dto.SubmissionRequest;
import intercoach.submission.dto.SubmissionResponse;
import intercoach.submission.model.Submission;
import intercoach.submission.model.SubmissionStatus;
import intercoach.submission.repository.SubmissionRepository;
import intercoach.user.model.AppUser;
import intercoach.user.repository.AppUserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final AppUserRepository appUserRepository;
    private final AiFeedbackService aiFeedbackService;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            AppUserRepository appUserRepository,
            AiFeedbackService aiFeedbackService
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.appUserRepository = appUserRepository;
        this.aiFeedbackService = aiFeedbackService;
    }

    public SubmissionResponse createSubmission(
            Long problemId,
            SubmissionRequest request
    ) {
        Submission savedSubmission = createPendingSubmission(problemId, request);
        try {
            AiFeedbackResponse feedback = aiFeedbackService.reviewSubmission(
                    savedSubmission.getProblem(), request.getSubmittedCode()
            );
            applyFeedback(savedSubmission, feedback);
        } catch (Exception exception) {
            savedSubmission.setStatus(SubmissionStatus.FAILED);
            savedSubmission.setAiFeedback(
                    "AI feedback could not be generated: " + exception.getMessage()
            );
        }
        return toResponse(submissionRepository.save(savedSubmission));
    }

    Submission createPendingSubmission(Long problemId, SubmissionRequest request) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Problem not found with id: " + problemId
                        )
                );

        AppUser user = appUserRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + request.getUserId()
                        )
                );

        Submission submission = new Submission();
        submission.setProblem(problem);
        submission.setUser(user);
        submission.setSubmittedCode(request.getSubmittedCode());
        submission.setLanguage(request.getLanguage());
        submission.setStatus(SubmissionStatus.PENDING);

        // Save first so the submission exists even if the AI request fails.
        return submissionRepository.save(submission);
    }

    public SubmissionResponse getSubmissionById(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Submission not found with id: " + submissionId
                        )
                );

        return toResponse(submission);
    }

    public List<SubmissionResponse> getSubmissionsForProblem(Long problemId) {
        if (!problemRepository.existsById(problemId)) {
            throw new ResourceNotFoundException(
                    "Problem not found with id: " + problemId
            );
        }

        return submissionRepository.findByProblemId(problemId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<SubmissionResponse> getSubmissionsForUser(Long userId) {
        if (!appUserRepository.existsById(userId)) {
            throw new ResourceNotFoundException(
                    "User not found with id: " + userId
            );
        }

        return submissionRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    SubmissionResponse toResponse(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getProblem().getId(),
                submission.getUser() == null
                        ? null
                        : submission.getUser().getId(),
                submission.getSubmittedCode(),
                submission.getLanguage(),
                submission.getStatus(),
                submission.getAiFeedback(),
                submission.getFeedbackSummary(),
                submission.getCorrectness(),
                submission.getBugs(),
                submission.getEdgeCases(),
                submission.getTimeComplexity(),
                submission.getSpaceComplexity(),
                submission.getHint(),
                submission.getSuggestedImprovement(),
                submission.getCreatedAt()
        );
    }

    static void applyFeedback(Submission submission, AiFeedbackResponse feedback) {
        submission.setFeedbackSummary(feedback.summary());
        submission.setCorrectness(feedback.correctness());
        submission.setBugs(joinList(feedback.bugs()));
        submission.setEdgeCases(joinList(feedback.edgeCases()));
        submission.setTimeComplexity(feedback.timeComplexity());
        submission.setSpaceComplexity(feedback.spaceComplexity());
        submission.setHint(feedback.hint());
        submission.setSuggestedImprovement(feedback.suggestedImprovement());
        // Keep the original field populated for backward compatibility.
        submission.setAiFeedback(feedback.summary());
        submission.setStatus(SubmissionStatus.REVIEWED);
    }

    private static String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return String.join("\n", values);
    }
}
