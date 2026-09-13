-- SIXPAY CONNECT canonical Accounting Flyway baseline
-- This file creates the complete current Accounting schema from an empty database.


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
    financial_snapshot_id UUID,
    financial_snapshot_version VARCHAR(32),
    financial_snapshot_finalized_at TIMESTAMPTZ,
    debtor_account_reference VARCHAR(256),
    creditor_account_reference VARCHAR(256),
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


-- ---------------------------------------------------------------------------
-- Accounting payment candidate projection
-- ---------------------------------------------------------------------------

CREATE TABLE accounting_payment_candidates (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    public_payment_reference VARCHAR(128) NOT NULL,
    partner_id VARCHAR(128) NOT NULL,
    financial_institution_code VARCHAR(64) NOT NULL,
    t0_outcome VARCHAR(32) NOT NULL,
    bank_reference VARCHAR(128) NOT NULL,
    t0_observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accounting_business_date DATE NOT NULL,
    financial_snapshot_id UUID NOT NULL,
    financial_snapshot_version VARCHAR(32) NOT NULL,
    financial_snapshot_finalized_at TIMESTAMP WITH TIME ZONE NOT NULL,
    debtor_account_reference VARCHAR(256) NOT NULL,
    creditor_account_reference VARCHAR(256) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    candidate_created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    batch_id UUID NULL,
    tresorpay_reference VARCHAR(128) NULL,
    tresorpay_transaction_id VARCHAR(128) NULL,
    tresorpay_status VARCHAR(64) NULL,
    tresorpay_payment_method VARCHAR(64) NULL,
    tresorpay_operator_reference VARCHAR(128) NULL,
    tresorpay_debit_effectue BOOLEAN NULL,
    tresorpay_quittance_disponible BOOLEAN NULL,
    tresorpay_provider_updated_at TIMESTAMP WITH TIME ZONE NULL,
    tresorpay_failure_reason VARCHAR(512) NULL,
    tresorpay_checked_at TIMESTAMP WITH TIME ZONE NULL,
    tresorpay_request_reference VARCHAR(128) NULL,
    tresorpay_correlation_id VARCHAR(128) NULL,
    CONSTRAINT uk_accounting_payment_candidates_event_id UNIQUE (event_id),
    CONSTRAINT uk_accounting_payment_candidates_business_identity UNIQUE (payment_id, financial_snapshot_id),
    CONSTRAINT ck_accounting_payment_candidates_t0_completed CHECK (t0_outcome = 'COMPLETED'),
    CONSTRAINT ck_accounting_payment_candidates_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_accounting_payment_candidates_selection
    ON accounting_payment_candidates(accounting_business_date, financial_institution_code, batch_id, tresorpay_status);

CREATE TABLE accounting_payment_candidate_entries (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL,
    entry_snapshot_id UUID NOT NULL,
    entry_sequence INTEGER NOT NULL,
    direction VARCHAR(16) NOT NULL,
    account_reference VARCHAR(256) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_accounting_candidate_entries_candidate
        FOREIGN KEY (candidate_id) REFERENCES accounting_payment_candidates(id) ON DELETE CASCADE,
    CONSTRAINT uk_accounting_candidate_entries_snapshot UNIQUE (candidate_id, entry_snapshot_id),
    CONSTRAINT uk_accounting_candidate_entries_sequence UNIQUE (candidate_id, entry_sequence),
    CONSTRAINT ck_accounting_candidate_entries_direction CHECK (direction IN ('DEBIT','CREDIT')),
    CONSTRAINT ck_accounting_candidate_entries_amount_positive CHECK (amount > 0)
);


-- ---------------------------------------------------------------------------
-- Accounting batch frozen-entry snapshots
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS accounting_batch_item_entries (
    id UUID PRIMARY KEY,
    batch_item_id UUID NOT NULL,
    entry_snapshot_id UUID NOT NULL,
    entry_sequence INTEGER NOT NULL,
    direction VARCHAR(16) NOT NULL,
    account_reference VARCHAR(256) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_accounting_batch_item_entries_item
        FOREIGN KEY (batch_item_id) REFERENCES accounting_batch_items(id) ON DELETE CASCADE,
    CONSTRAINT uk_accounting_batch_item_entries_snapshot UNIQUE (batch_item_id, entry_snapshot_id),
    CONSTRAINT uk_accounting_batch_item_entries_sequence UNIQUE (batch_item_id, entry_sequence),
    CONSTRAINT ck_accounting_batch_item_entries_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_accounting_batch_item_entries_amount_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_accounting_batch_item_entries_item
    ON accounting_batch_item_entries(batch_item_id);

CREATE INDEX IF NOT EXISTS idx_accounting_batch_items_financial_snapshot
    ON accounting_batch_items(financial_snapshot_id);


-- ---------------------------------------------------------------------------
-- Accounting TFJ confirmations
-- ---------------------------------------------------------------------------

CREATE TABLE accounting_tfj_confirmations (
    confirmation_id UUID PRIMARY KEY,
    idempotency_key VARCHAR(200) NOT NULL,
    logical_payload_hash VARCHAR(64) NOT NULL,
    financial_institution_code VARCHAR(35) NOT NULL,
    business_date DATE NOT NULL,
    payment_reference VARCHAR(100) NOT NULL,
    bank_posting_reference VARCHAR(128) NOT NULL,
    tfj_batch_reference VARCHAR(100),
    tfj_status VARCHAR(16) NOT NULL,
    confirmed_at TIMESTAMPTZ NOT NULL,
    failure_code VARCHAR(64),
    failure_description VARCHAR(500),
    recovery_action VARCHAR(32),
    received_at TIMESTAMPTZ NOT NULL,
    observation_channel VARCHAR(32) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    match_status VARCHAR(16) NOT NULL,
    matched_payment_id UUID,
    finality_published_at TIMESTAMPTZ,
    CONSTRAINT uk_accounting_tfj_confirmation_idempotency
        UNIQUE (idempotency_key),
    CONSTRAINT ck_accounting_tfj_status
        CHECK (
            tfj_status IN ('PENDING', 'INTEGRATED', 'FAILED')
        ),
    CONSTRAINT ck_accounting_tfj_match_status
        CHECK (
            match_status IN ('MATCHED', 'UNMATCHED', 'AMBIGUOUS')
        ),
    CONSTRAINT ck_accounting_tfj_observation_channel
        CHECK (
            observation_channel IN (
                'ASYNC_CALLBACK',
                'SCHEDULED_LOOKUP'
            )
        ),
    CONSTRAINT ck_accounting_tfj_failure
        CHECK (
            (
                tfj_status = 'FAILED'
                AND failure_code IS NOT NULL
                AND failure_description IS NOT NULL
                AND recovery_action IS NOT NULL
            )
            OR
            (
                tfj_status <> 'FAILED'
                AND failure_code IS NULL
                AND failure_description IS NULL
                AND recovery_action IS NULL
            )
        ),
    CONSTRAINT ck_accounting_tfj_match_payment
        CHECK (
            (
                match_status = 'MATCHED'
                AND matched_payment_id IS NOT NULL
            )
            OR
            (
                match_status <> 'MATCHED'
                AND matched_payment_id IS NULL
            )
        )
);

CREATE INDEX idx_accounting_tfj_matching
    ON accounting_tfj_confirmations (
        financial_institution_code,
        business_date,
        payment_reference,
        bank_posting_reference
    );

CREATE INDEX idx_accounting_tfj_unpublished_finality
    ON accounting_tfj_confirmations (
        finality_published_at,
        received_at
    )
    WHERE matched_payment_id IS NOT NULL
      AND tfj_status IN ('INTEGRATED', 'FAILED');
