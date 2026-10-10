CREATE TABLE submission_review_job (
    submission_id BIGINT PRIMARY KEY REFERENCES submission(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'READY'
        CHECK (status IN ('READY', 'RUNNING', 'COMPLETED', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    available_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_until TIMESTAMP WITH TIME ZONE,
    claim_token UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((status = 'RUNNING' AND lease_until IS NOT NULL AND claim_token IS NOT NULL)
        OR (status <> 'RUNNING' AND lease_until IS NULL AND claim_token IS NULL))
);

CREATE INDEX idx_review_job_ready ON submission_review_job(available_at, submission_id)
    WHERE status = 'READY';
CREATE INDEX idx_review_job_lease ON submission_review_job(lease_until, submission_id)
    WHERE status = 'RUNNING';
