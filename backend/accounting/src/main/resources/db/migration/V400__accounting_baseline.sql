-- SIXPAY CONNECT canonical pre-production Flyway baseline
-- FS-2.3 Database baseline consolidation
-- This file represents current schema state; Git preserves prior migration history.


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V202608071100__accounting_batches.sql
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS accounting_batches (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL,
    business_date DATE NOT NULL,
    financial_institution_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_accounting_batches_idempotency_key
        UNIQUE (idempotency_key),
    CONSTRAINT ck_accounting_batches_status
        CHECK (status IN ('COMPLETED', 'NOT_COMPLETED'))
);

CREATE TABLE IF NOT EXISTS accounting_batch_items (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    public_payment_reference VARCHAR(128) NOT NULL,
    partner_id VARCHAR(128) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_occurred_at TIMESTAMPTZ NOT NULL,
    payment_business_date DATE NOT NULL,
    bank_posting_reference VARCHAR(128),
    tresorpay_status VARCHAR(64) NOT NULL,
    tresorpay_status_checked_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT fk_accounting_batch_items_batch
        FOREIGN KEY (batch_id)
        REFERENCES accounting_batches(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_accounting_batch_items_payment_id
        UNIQUE (payment_id),
    CONSTRAINT ck_accounting_batch_items_amount
        CHECK (amount > 0),
    CONSTRAINT ck_accounting_batch_items_currency
        CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_accounting_batch_items_status
        CHECK (
            status IN (
                'PENDING',
                'COMPLETED',
                'REJECTED',
                'RECONCILIATION_REQUIRED'
            )
        )
);

CREATE INDEX IF NOT EXISTS idx_accounting_batches_business_date
    ON accounting_batches (
        business_date,
        financial_institution_code
    );

CREATE INDEX IF NOT EXISTS idx_accounting_batch_items_batch_id
    ON accounting_batch_items (batch_id);

CREATE INDEX IF NOT EXISTS idx_accounting_batch_items_status
    ON accounting_batch_items (status);


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V202608071200__accounting_batch_tracking.sql
-- ---------------------------------------------------------------------------

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
