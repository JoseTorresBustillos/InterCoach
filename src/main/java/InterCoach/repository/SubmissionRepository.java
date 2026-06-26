package InterCoach.repository;

import InterCoach.model.Submission;
import InterCoach.model.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByProblemId(Long problemId);

    //Used tp find weak areas based on failed or reviewed submissions
    List<Submission> findByStatus(SubmissionStatus status);
}