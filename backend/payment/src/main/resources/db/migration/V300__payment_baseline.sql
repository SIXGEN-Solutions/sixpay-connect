-- SIXPAY CONNECT canonical pre-production Flyway baseline
-- FS-2.3 Database baseline consolidation
-- This file represents current schema state; Git preserves prior migration history.


-- ---------------------------------------------------------------------------
-- Source folded into baseline: final Payment aggregate persistence
-- ---------------------------------------------------------------------------

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
            AND (state_payload ->> 'schemaVersion')::INTEGER IN (1, 2)
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


-- ---------------------------------------------------------------------------
-- Source folded into baseline: payment audit
-- ---------------------------------------------------------------------------

CREATE TABLE payment_audit
(
    event_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    public_payment_reference VARCHAR(30) NOT NULL,
    event_type VARCHAR(96) NOT NULL,
    payment_status VARCHAR(48) NOT NULL,
    business_version BIGINT NOT NULL,
    event_sequence INTEGER NOT NULL,
    correlation_id VARCHAR(150) NOT NULL,
    causation_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_payment_audit PRIMARY KEY (event_id),
    CONSTRAINT fk_payment_audit_payment
        FOREIGN KEY (payment_id) REFERENCES payments (payment_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uk_payment_audit_payment_version_sequence
        UNIQUE (payment_id, business_version, event_sequence),
    CONSTRAINT ck_payment_audit_public_reference
        CHECK (public_payment_reference ~ '^PAY-[0-9A-HJKMNP-TV-Z]{26}$'),
    CONSTRAINT ck_payment_audit_business_version_positive CHECK (business_version > 0),
    CONSTRAINT ck_payment_audit_event_sequence_positive CHECK (event_sequence > 0),
    CONSTRAINT ck_payment_audit_event_type_not_blank CHECK (btrim(event_type) <> ''),
    CONSTRAINT ck_payment_audit_correlation_not_blank CHECK (btrim(correlation_id) <> '')
);

CREATE INDEX idx_payment_audit_payment_occurred_at
    ON payment_audit (payment_id, occurred_at);
CREATE INDEX idx_payment_audit_correlation_id
    ON payment_audit (correlation_id);
CREATE INDEX idx_payment_audit_event_type
    ON payment_audit (event_type);

CREATE OR REPLACE FUNCTION reject_payment_audit_mutation()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'payment_audit is append-only; % is forbidden', TG_OP;
END;
$$;

CREATE TRIGGER trg_payment_audit_reject_update
    BEFORE UPDATE ON payment_audit
    FOR EACH ROW EXECUTE FUNCTION reject_payment_audit_mutation();

CREATE TRIGGER trg_payment_audit_reject_delete
    BEFORE DELETE ON payment_audit
    FOR EACH ROW EXECUTE FUNCTION reject_payment_audit_mutation();


-- ---------------------------------------------------------------------------
-- Source folded into baseline: payment outbox
-- ---------------------------------------------------------------------------

CREATE TABLE payment_outbox_events
(
    event_id          UUID          NOT NULL,
    aggregate_type    VARCHAR(64)   NOT NULL,
    aggregate_id      UUID          NOT NULL,
    event_type        VARCHAR(150)  NOT NULL,
    schema_version    INTEGER       NOT NULL,
    correlation_id    VARCHAR(150)  NOT NULL,
    payload           JSONB         NOT NULL,
    status            VARCHAR(16)   NOT NULL,
    occurred_at       TIMESTAMPTZ   NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL,
    published_at      TIMESTAMPTZ,
    failure_reason    VARCHAR(1000),
    attempt_count     INTEGER       NOT NULL DEFAULT 0,
    next_attempt_at   TIMESTAMPTZ   NOT NULL,
    last_attempt_at   TIMESTAMPTZ,
    claimed_at        TIMESTAMPTZ,
    claimed_by        VARCHAR(100),

    CONSTRAINT pk_payment_outbox_events PRIMARY KEY (event_id),

    CONSTRAINT fk_payment_outbox_payment
        FOREIGN KEY (aggregate_id)
        REFERENCES payments (payment_id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT ck_payment_outbox_aggregate_type
        CHECK (aggregate_type = 'PAYMENT'),

    CONSTRAINT ck_payment_outbox_event_type
        CHECK (btrim(event_type) <> ''),

    CONSTRAINT ck_payment_outbox_schema_version
        CHECK (schema_version > 0),

    CONSTRAINT ck_payment_outbox_correlation
        CHECK (btrim(correlation_id) <> ''),

    CONSTRAINT ck_payment_outbox_payload_object
        CHECK (jsonb_typeof(payload) = 'object'),

    CONSTRAINT ck_payment_outbox_status
        CHECK (status IN (
            'PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED', 'DEAD'
        )),

    CONSTRAINT ck_payment_outbox_attempt_count
        CHECK (attempt_count >= 0),

    CONSTRAINT ck_payment_outbox_created_after_occurrence
        CHECK (created_at >= occurred_at),

    CONSTRAINT ck_payment_outbox_publication_state
        CHECK (
            (status = 'PUBLISHED' AND published_at IS NOT NULL)
            OR
            (status <> 'PUBLISHED' AND published_at IS NULL)
        ),

    CONSTRAINT ck_payment_outbox_claim_state
        CHECK (
            (
                status = 'PROCESSING'
                AND claimed_at IS NOT NULL
                AND claimed_by IS NOT NULL
            )
            OR
            (
                status <> 'PROCESSING'
                AND claimed_at IS NULL
                AND claimed_by IS NULL
            )
        )
);

CREATE INDEX idx_payment_outbox_claimable
    ON payment_outbox_events (status, next_attempt_at, occurred_at);

CREATE INDEX idx_payment_outbox_aggregate
    ON payment_outbox_events (aggregate_id, occurred_at);

CREATE INDEX idx_payment_outbox_correlation
    ON payment_outbox_events (correlation_id);

COMMENT ON TABLE payment_outbox_events IS
    'Durable transport-neutral Payment outbox. Lot 3.4 performs no broker publication.';


-- ---------------------------------------------------------------------------
-- Source folded into baseline: payment idempotency
-- ---------------------------------------------------------------------------

CREATE TABLE payment_idempotency
(
    id                  UUID          NOT NULL,
    operation           VARCHAR(160)  NOT NULL,
    idempotency_key     VARCHAR(150)  NOT NULL,
    request_hash        VARCHAR(64)   NOT NULL,
    status              VARCHAR(16)   NOT NULL,
    payment_id          UUID,
    response_status     VARCHAR(64),
    response_payload    JSONB,
    failure_reason      VARCHAR(1000),
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL,
    completed_at        TIMESTAMPTZ,
    persistence_version BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_payment_idempotency
        PRIMARY KEY (id),

    CONSTRAINT uk_payment_idempotency_operation_key
        UNIQUE (operation, idempotency_key),

    CONSTRAINT fk_payment_idempotency_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments (payment_id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT ck_payment_idempotency_operation
        CHECK (btrim(operation) <> ''),

    CONSTRAINT ck_payment_idempotency_key
        CHECK (btrim(idempotency_key) <> ''),

    CONSTRAINT ck_payment_idempotency_request_hash
        CHECK (request_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT ck_payment_idempotency_status
        CHECK (
            status IN (
                'IN_PROGRESS',
                'COMPLETED',
                'FAILED'
            )
        ),

    CONSTRAINT ck_payment_idempotency_updated_after_created
        CHECK (updated_at >= created_at),

    CONSTRAINT ck_payment_idempotency_completed_state
        CHECK (
            (
                status = 'COMPLETED'
                AND payment_id IS NOT NULL
                AND response_status IS NOT NULL
                AND response_payload IS NOT NULL
                AND completed_at IS NOT NULL
                AND failure_reason IS NULL
            )
            OR
            (
                status = 'IN_PROGRESS'
                AND payment_id IS NULL
                AND response_status IS NULL
                AND response_payload IS NULL
                AND completed_at IS NULL
                AND failure_reason IS NULL
            )
            OR
            (
                status = 'FAILED'
                AND payment_id IS NULL
                AND response_status IS NULL
                AND response_payload IS NULL
                AND completed_at IS NULL
                AND failure_reason IS NOT NULL
            )
        ),

    CONSTRAINT ck_payment_idempotency_response_payload_object
        CHECK (
            response_payload IS NULL
            OR jsonb_typeof(response_payload) = 'object'
        )
);

CREATE INDEX idx_payment_idempotency_status_updated
    ON payment_idempotency (
        status,
        updated_at
    );

CREATE INDEX idx_payment_idempotency_payment
    ON payment_idempotency (payment_id);

COMMENT ON TABLE payment_idempotency IS
    'Durable Payment idempotency reservation and replay result.';

COMMENT ON COLUMN payment_idempotency.request_hash IS
    'Lowercase SHA-256 of the canonical Payment request representation.';

COMMENT ON COLUMN payment_idempotency.response_payload IS
    'Exact replayable application response persisted after successful completion.';


-- ---------------------------------------------------------------------------
-- Source folded into baseline: Payment-owned observed-customer association
-- ---------------------------------------------------------------------------

CREATE TABLE payment_observed_customer_link (
    payment_id UUID PRIMARY KEY,
    observed_customer_id UUID NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_observed_customer_link_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(payment_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_payment_observed_customer_link_customer
    ON payment_observed_customer_link(observed_customer_id, payment_id);
