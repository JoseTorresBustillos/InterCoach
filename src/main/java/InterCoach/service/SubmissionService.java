package InterCoach.service;

import InterCoach.dto.AiFeedbackResponse;
import InterCoach.dto.SubmissionRequest;
import InterCoach.dto.SubmissionResponse;
import InterCoach.exception.ResourceNotFoundException;
import InterCoach.model.AppUser;
import InterCoach.model.Problem;
import InterCoach.model.Submission;
import InterCoach.model.SubmissionStatus;
import InterCoach.repository.AppUserRepository;
import InterCoach.repository.ProblemRepository;
import InterCoach.repository.SubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
        Submission savedSubmission = submissionRepository.save(submission);

        try {
            AiFeedbackResponse feedback =
                    aiFeedbackService.reviewSubmission(
                            problem,
                            request.getSubmittedCode()
                    );

            savedSubmission.setFeedbackSummary(feedback.summary());
            savedSubmission.setCorrectness(feedback.correctness());
            savedSubmission.setBugs(joinList(feedback.bugs()));
            savedSubmission.setEdgeCases(joinList(feedback.edgeCases()));
            savedSubmission.setTimeComplexity(feedback.timeComplexity());
            savedSubmission.setSpaceComplexity(feedback.spaceComplexity());
            savedSubmission.setHint(feedback.hint());
            savedSubmission.setSuggestedImprovement(
                    feedback.suggestedImprovement()
            );

            // Keep the original field populated for backward compatibility.
            savedSubmission.setAiFeedback(feedback.summary());
            savedSubmission.setStatus(SubmissionStatus.REVIEWED);
        } catch (Exception exception) {
            savedSubmission.setStatus(SubmissionStatus.FAILED);
            savedSubmission.setAiFeedback(
                    "AI feedback could not be generated: "
                            + exception.getMessage()
            );
        }

        return toResponse(submissionRepository.save(savedSubmission));
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

    private SubmissionResponse toResponse(Submission submission) {
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

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return String.join("\n", values);
    }
}
