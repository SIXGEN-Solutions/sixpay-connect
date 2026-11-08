-- Durable, Payment-owned T0 lookup scheduling. No financial POST or outbox state here.
CREATE TABLE payment_t0_recovery_attempts (
    payment_id UUID PRIMARY KEY REFERENCES payments(payment_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    first_posting_at TIMESTAMPTZ NOT NULL,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    claim_token UUID,
    claimed_at TIMESTAMPTZ,
    claimed_by VARCHAR(150),
    resolved_at TIMESTAMPTZ,
    CONSTRAINT ck_payment_t0_recovery_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_payment_t0_recovery_claim_consistency CHECK (
        (claim_token IS NULL AND claimed_at IS NULL AND claimed_by IS NULL)
        OR (claim_token IS NOT NULL AND claimed_at IS NOT NULL AND claimed_by IS NOT NULL)
    ),
    CONSTRAINT ck_payment_t0_recovery_resolved_claim CHECK (
        resolved_at IS NULL OR claim_token IS NULL
    )
);
CREATE INDEX idx_payment_t0_recovery_due
    ON payment_t0_recovery_attempts (next_attempt_at, first_posting_at)
    WHERE resolved_at IS NULL;
COMMENT ON TABLE payment_t0_recovery_attempts IS
    'Payment-owned, lookup-only T0 recovery lease/retry state; no financial command replay.';
