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
