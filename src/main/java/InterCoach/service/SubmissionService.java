package InterCoach.service;

import InterCoach.dto.SubmissionRequest;
import InterCoach.dto.SubmissionResponse;
import InterCoach.model.Problem;
import InterCoach.model.Submission;
import InterCoach.model.SubmissionStatus;
import InterCoach.repository.ProblemRepository;
import InterCoach.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import InterCoach.dto.AiFeedbackResponse;
import java.util.StringJoiner;

import java.util.List;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final AiFeedbackService aiFeedbackService;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            AiFeedbackService aiFeedbackService
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.aiFeedbackService = aiFeedbackService;
    }

    public SubmissionResponse createSubmission(Long problemId, SubmissionRequest request) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found with id: " + problemId));

        Submission submission = new Submission();
        submission.setProblem(problem);
        submission.setSubmittedCode(request.getSubmittedCode());
        submission.setLanguage(request.getLanguage());
        submission.setStatus(SubmissionStatus.PENDING);

        Submission savedSubmission = submissionRepository.save(submission);

        try {
            // Review after saving so the submission exists even if the AI request fails.
            AiFeedbackResponse feedback = aiFeedbackService.reviewSubmission(problem, request.getSubmittedCode());

           // Store both structured fields and a readable fallback summary.
            savedSubmission.setFeedbackSummary(feedback.summary());
            savedSubmission.setCorrectness(feedback.correctness());
            savedSubmission.setBugs(joinList(feedback.bugs()));
            savedSubmission.setEdgeCases(joinList(feedback.edgeCases()));
            savedSubmission.setTimeComplexity(feedback.timeComplexity());
            savedSubmission.setSpaceComplexity(feedback.spaceComplexity());
            savedSubmission.setHint(feedback.hint());
            savedSubmission.setSuggestedImprovement(feedback.suggestedImprovement());

savedSubmission.setAiFeedback(feedback.summary());
savedSubmission.setStatus(SubmissionStatus.REVIEWED);
        } catch (Exception e) {
            // Keep failed AI calls visible without losing the user's submitted code.
            savedSubmission.setStatus(SubmissionStatus.FAILED);
            savedSubmission.setAiFeedback("AI feedback failed: " + e.getMessage());

            savedSubmission = submissionRepository.save(savedSubmission);
        }

        return toResponse(savedSubmission);
    }

    public SubmissionResponse getSubmissionById(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found with id: " + submissionId));

        return toResponse(submission);
    }

    public List<SubmissionResponse> getSubmissionsForProblem(Long problemId) {
        if (!problemRepository.existsById(problemId)) {
            throw new RuntimeException("Problem not found with id: " + problemId);
        }

        return submissionRepository.findByProblemId(problemId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

   private SubmissionResponse toResponse(Submission submission) {
    return new SubmissionResponse(
            submission.getId(),
            submission.getProblem().getId(),
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
    private String joinList(List<String> items) {
    if (items == null || items.isEmpty()) {
        return "";
    }

    StringJoiner joiner = new StringJoiner("\n");
    for (String item : items) {
        joiner.add("- " + item);
    }

    return joiner.toString();
}
}