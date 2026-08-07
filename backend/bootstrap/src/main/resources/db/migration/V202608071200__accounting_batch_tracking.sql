CREATE TABLE IF NOT EXISTS accounting_batch_tracking (
    batch_id UUID PRIMARY KEY,
    submission_state VARCHAR(48) NOT NULL,
    provider_batch_reference VARCHAR(128),
    last_submission_attempt_at TIMESTAMPTZ,
    last_reconciliation_at TIMESTAMPTZ,
    reconciliation_attempts INTEGER NOT NULL DEFAULT 0,
    last_error_code VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_accounting_batch_tracking_batch
        FOREIGN KEY (batch_id)
        REFERENCES accounting_batches(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_accounting_batch_tracking_state
        CHECK (
            submission_state IN (
                'READY',
                'SUBMITTING',
                'SUBMITTED',
                'OUTCOME_UNKNOWN',
                'COMPLETED',
                'REJECTED',
                'RECONCILIATION_REQUIRED'
            )
        ),
    CONSTRAINT ck_accounting_batch_tracking_attempts
        CHECK (reconciliation_attempts >= 0)
);

CREATE TABLE IF NOT EXISTS accounting_batch_item_tracking (
    payment_id UUID PRIMARY KEY,
    batch_id UUID NOT NULL,
    provider_item_reference VARCHAR(128),
    rejection_code VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_accounting_item_tracking_batch
        FOREIGN KEY (batch_id)
        REFERENCES accounting_batch_tracking(batch_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_accounting_item_tracking_payment
        FOREIGN KEY (payment_id)
        REFERENCES accounting_batch_items(payment_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_accounting_tracking_state
    ON accounting_batch_tracking (
        submission_state,
        last_reconciliation_at
    );

CREATE INDEX IF NOT EXISTS idx_accounting_item_tracking_batch
    ON accounting_batch_item_tracking (batch_id);
