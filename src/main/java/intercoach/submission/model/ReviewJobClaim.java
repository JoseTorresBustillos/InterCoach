package intercoach.submission.model;

import java.util.UUID;

public record ReviewJobClaim(Long submissionId, UUID token, int attemptCount) {
}
