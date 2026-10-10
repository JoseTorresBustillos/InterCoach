package intercoach.submission.repository;

import intercoach.submission.model.Submission;
import intercoach.submission.model.SubmissionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByProblemId(Long problemId);

    List<Submission> findByUserId(Long userId);

    @EntityGraph(attributePaths = "problem")
    @Query("select submission from Submission submission where submission.id = :id")
    Optional<Submission> findForReviewById(@Param("id") Long id);

    @EntityGraph(attributePaths = "problem")
    List<Submission> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select submission.user.id from Submission submission where submission.id = :submissionId")
    Optional<Long> findUserIdById(@Param("submissionId") Long submissionId);

    // Recommendation logic uses submission outcomes to detect weak categories.
    List<Submission> findByStatus(SubmissionStatus status);
}
