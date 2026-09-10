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
