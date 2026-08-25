package intercoach.service;

import intercoach.model.Problem;
import intercoach.model.Submission;
import intercoach.model.SubmissionStatus;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class SubmissionInsightService {

    public boolean isWeakSubmission(Submission submission) {
        return submission.getStatus() == SubmissionStatus.FAILED
                || (submission.getStatus() == SubmissionStatus.REVIEWED
                        && hasSignsOfWeakness(submission));
    }

    public boolean isStrongReviewedSubmission(Submission submission) {
        return submission.getStatus() == SubmissionStatus.REVIEWED
                && !hasSignsOfWeakness(submission);
    }

    public String categoryFor(Submission submission) {
        Problem problem = submission.getProblem();

        return problem == null ? null : problem.getCategory();
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

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
