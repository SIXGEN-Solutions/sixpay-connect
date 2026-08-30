-- PAYMENT_COMPLETION LOT 0.4.4
-- Durable technical recovery state for external Payment operations.
-- OUTCOME_UNKNOWN is not a Payment or ConfirmationChallenge business status.

ALTER TABLE payment_idempotency
    ADD COLUMN recovery_reference VARCHAR(150),
    ADD COLUMN recovery_reason VARCHAR(1000),
    ADD COLUMN unknown_outcome_at TIMESTAMPTZ;

ALTER TABLE payment_idempotency
    DROP CONSTRAINT ck_payment_idempotency_status;

ALTER TABLE payment_idempotency
    ADD CONSTRAINT ck_payment_idempotency_status
        CHECK (
            status IN (
                'IN_PROGRESS',
                'OUTCOME_UNKNOWN',
                'COMPLETED',
                'FAILED'
            )
        );

ALTER TABLE payment_idempotency
    DROP CONSTRAINT ck_payment_idempotency_completed_state;

ALTER TABLE payment_idempotency
    ADD CONSTRAINT ck_payment_idempotency_completed_state
        CHECK (
            (
                status = 'COMPLETED'
                AND payment_id IS NOT NULL
                AND response_status IS NOT NULL
                AND response_payload IS NOT NULL
                AND completed_at IS NOT NULL
                AND failure_reason IS NULL
                AND recovery_reference IS NULL
                AND recovery_reason IS NULL
                AND unknown_outcome_at IS NULL
            )
            OR
            (
                status = 'IN_PROGRESS'
                AND payment_id IS NULL
                AND response_status IS NULL
                AND response_payload IS NULL
                AND completed_at IS NULL
                AND failure_reason IS NULL
                AND recovery_reference IS NULL
                AND recovery_reason IS NULL
                AND unknown_outcome_at IS NULL
            )
            OR
            (
                status = 'OUTCOME_UNKNOWN'
                AND payment_id IS NOT NULL
                AND response_status IS NULL
                AND response_payload IS NULL
                AND completed_at IS NULL
                AND failure_reason IS NULL
                AND recovery_reason IS NOT NULL
                AND unknown_outcome_at IS NOT NULL
            )
            OR
            (
                status = 'FAILED'
                AND payment_id IS NULL
                AND response_status IS NULL
                AND response_payload IS NULL
                AND completed_at IS NULL
                AND failure_reason IS NOT NULL
                AND recovery_reference IS NULL
                AND recovery_reason IS NULL
                AND unknown_outcome_at IS NULL
            )
        );

CREATE INDEX idx_payment_idempotency_unknown_recovery
    ON payment_idempotency (status, unknown_outcome_at)
    WHERE status = 'OUTCOME_UNKNOWN';

COMMENT ON COLUMN payment_idempotency.recovery_reference IS
    'Optional external reference known when an operation outcome becomes uncertain.';

COMMENT ON COLUMN payment_idempotency.recovery_reason IS
    'Sanitized technical reason requiring authoritative recovery; must contain no OTP or secret.';

COMMENT ON COLUMN payment_idempotency.unknown_outcome_at IS
    'Instant at which SIXPAY requires authoritative lookup before any retry decision.';
