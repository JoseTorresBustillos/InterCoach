package intercoach.submission.repository;

import intercoach.submission.model.ReviewJobClaim;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewJobRepository {

    public static final String FAILURE_MESSAGE =
            "AI feedback could not be generated after repeated attempts. Please try again later.";

    private final NamedParameterJdbcTemplate jdbc;

    public ReviewJobRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void enqueue(Long submissionId) {
        jdbc.update("""
                INSERT INTO submission_review_job (submission_id) VALUES (:id)
                ON CONFLICT (submission_id) DO NOTHING
                """, Map.of("id", submissionId));
    }

    public Optional<ReviewJobClaim> claim(int maxAttempts, int leaseSeconds) {
        // One statement claims one eligible row without waiting for another worker's row lock.
        return jdbc.query("""
                WITH candidate AS (
                    SELECT submission_id FROM submission_review_job
                    WHERE attempt_count < :maxAttempts
                        AND ((status = 'READY' AND available_at <= CURRENT_TIMESTAMP)
                            OR (status = 'RUNNING' AND lease_until <= CURRENT_TIMESTAMP))
                    ORDER BY available_at, submission_id
                    LIMIT 1 FOR UPDATE SKIP LOCKED
                )
                UPDATE submission_review_job job
                SET status = 'RUNNING', attempt_count = attempt_count + 1,
                    claim_token = :token,
                    lease_until = CURRENT_TIMESTAMP + (:leaseSeconds * INTERVAL '1 second'),
                    updated_at = CURRENT_TIMESTAMP
                FROM candidate WHERE job.submission_id = candidate.submission_id
                RETURNING job.submission_id, job.claim_token, job.attempt_count
                """, Map.of(
                "maxAttempts", maxAttempts, "leaseSeconds", leaseSeconds,
                "token", UUID.randomUUID()
        ), (row, index) -> new ReviewJobClaim(
                row.getLong("submission_id"),
                row.getObject("claim_token", UUID.class),
                row.getInt("attempt_count")
        )).stream().findFirst();
    }

    public boolean complete(ReviewJobClaim claim) {
        return jdbc.update("""
                UPDATE submission_review_job
                SET status = 'COMPLETED', lease_until = NULL, claim_token = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE submission_id = :id AND claim_token = :token
                    AND status = 'RUNNING' AND lease_until > CURRENT_TIMESTAMP
                """, claimParameters(claim)) == 1;
    }

    public boolean fail(ReviewJobClaim claim, boolean exhausted, int retryDelaySeconds) {
        return jdbc.update("""
                UPDATE submission_review_job
                SET status = :status, lease_until = NULL, claim_token = NULL,
                    available_at = CURRENT_TIMESTAMP + (:delay * INTERVAL '1 second'),
                    updated_at = CURRENT_TIMESTAMP
                WHERE submission_id = :id AND claim_token = :token
                    AND status = 'RUNNING' AND lease_until > CURRENT_TIMESTAMP
                """, Map.of(
                "id", claim.submissionId(), "token", claim.token(),
                "status", exhausted ? "FAILED" : "READY", "delay", retryDelaySeconds
        )) == 1;
    }

    public void failExpiredExhaustedJobs(int maxAttempts) {
        // Crashes on the final attempt must not leave a submission pending forever.
        jdbc.update("""
                WITH expired AS (
                    SELECT submission_id FROM submission_review_job
                    WHERE attempt_count >= :maxAttempts
                        AND ((status = 'RUNNING' AND lease_until <= CURRENT_TIMESTAMP)
                            OR (status = 'READY' AND available_at <= CURRENT_TIMESTAMP))
                    ORDER BY submission_id LIMIT 100 FOR UPDATE SKIP LOCKED
                ), failed AS (
                    UPDATE submission_review_job job
                    SET status = 'FAILED', lease_until = NULL, claim_token = NULL,
                        updated_at = CURRENT_TIMESTAMP
                    FROM expired WHERE job.submission_id = expired.submission_id
                    RETURNING job.submission_id
                )
                UPDATE submission SET status = 'FAILED', ai_feedback = :message
                WHERE id IN (SELECT submission_id FROM failed)
                """, Map.of("maxAttempts", maxAttempts, "message", FAILURE_MESSAGE));
    }

    private Map<String, Object> claimParameters(ReviewJobClaim claim) {
        return Map.of("id", claim.submissionId(), "token", claim.token());
    }
}
