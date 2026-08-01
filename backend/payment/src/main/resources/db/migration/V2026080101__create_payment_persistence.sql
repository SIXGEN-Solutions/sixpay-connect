CREATE TABLE payments
(
    payment_id                     UUID           NOT NULL,
    public_payment_reference       VARCHAR(30)    NOT NULL,
    payment_source                 VARCHAR(32)    NOT NULL,
    external_payment_reference     VARCHAR(128)   NOT NULL,
    external_subscription_reference VARCHAR(128)  NOT NULL,
    financial_institution_code     VARCHAR(32)    NOT NULL,
    requested_amount               NUMERIC(38,18) NOT NULL,
    requested_currency             VARCHAR(3)        NOT NULL,
    status                         VARCHAR(48)    NOT NULL,
    business_version               BIGINT         NOT NULL,
    received_at                    TIMESTAMPTZ    NOT NULL,
    updated_at                     TIMESTAMPTZ    NOT NULL,
    finalized_at                   TIMESTAMPTZ,
    state_payload                  JSONB          NOT NULL,
    persistence_version            BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT pk_payments
        PRIMARY KEY (payment_id),

    CONSTRAINT uk_payments_public_reference
        UNIQUE (public_payment_reference),

    CONSTRAINT uk_payments_source_external_reference
        UNIQUE (payment_source, external_payment_reference),

    CONSTRAINT ck_payments_source
        CHECK (payment_source = 'TRESOR_PAY'),

    CONSTRAINT ck_payments_public_reference
        CHECK (
            public_payment_reference
                ~ '^PAY-[0-9A-HJKMNP-TV-Z]{26}$'
        ),

    CONSTRAINT ck_payments_requested_amount_positive
        CHECK (requested_amount > 0),

    CONSTRAINT ck_payments_requested_currency
        CHECK (requested_currency ~ '^[A-Z]{3}$'),

    CONSTRAINT ck_payments_business_version_positive
        CHECK (business_version > 0),

    CONSTRAINT ck_payments_updated_after_received
        CHECK (updated_at >= received_at),

    CONSTRAINT ck_payments_finalized_after_updated
        CHECK (
            finalized_at IS NULL
            OR finalized_at >= updated_at
        ),

    CONSTRAINT ck_payments_terminal_finality
        CHECK (
            (
                status IN (
                    'REJECTED',
                    'FAILED',
                    'TREASURY_INTEGRATED',
                    'REVERSED'
                )
                AND finalized_at IS NOT NULL
            )
            OR
            (
                status NOT IN (
                    'REJECTED',
                    'FAILED',
                    'TREASURY_INTEGRATED',
                    'REVERSED'
                )
                AND finalized_at IS NULL
            )
        ),

    CONSTRAINT ck_payments_state_payload_object
        CHECK (jsonb_typeof(state_payload) = 'object'),

    CONSTRAINT ck_payments_state_schema_version
        CHECK (
            state_payload ? 'schemaVersion'
            AND (state_payload ->> 'schemaVersion')::INTEGER = 1
        )
);

CREATE INDEX idx_payments_status_updated_at
    ON payments (status, updated_at);

CREATE INDEX idx_payments_subscription_reference
    ON payments (external_subscription_reference);

CREATE INDEX idx_payments_financial_institution
    ON payments (financial_institution_code);

CREATE INDEX idx_payments_state_payload_gin
    ON payments
    USING GIN (state_payload jsonb_path_ops);

COMMENT ON TABLE payments IS
    'Complete Payment aggregate persistence. Financial workflow side effects are not executed by this table or repository.';

COMMENT ON COLUMN payments.business_version IS
    'Domain-owned Payment business version. Distinct from JPA persistence_version.';

COMMENT ON COLUMN payments.persistence_version IS
    'JPA optimistic-lock version used only for concurrent database updates.';

COMMENT ON COLUMN payments.state_payload IS
    'Versioned JSONB document containing the complete immutable PaymentState required by Payment.reconstitute.';
