package InterCoach.repository;

import InterCoach.model.Submission;
import InterCoach.model.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByProblemId(Long problemId);

    List<Submission> findByUserId(Long userId);

    // Recommendation logic uses submission outcomes to detect weak categories.
    List<Submission> findByStatus(SubmissionStatus status);
}