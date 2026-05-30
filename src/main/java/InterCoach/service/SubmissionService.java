package InterCoach.service;

import InterCoach.dto.SubmissionRequest;
import InterCoach.dto.SubmissionResponse;
import InterCoach.model.Problem;
import InterCoach.model.Submission;
import InterCoach.model.SubmissionStatus;
import InterCoach.repository.ProblemRepository;
import InterCoach.repository.SubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
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
                submission.getTimeComplexity(),
                submission.getSpaceComplexity(),
                submission.getCreatedAt()
        );
    }
}