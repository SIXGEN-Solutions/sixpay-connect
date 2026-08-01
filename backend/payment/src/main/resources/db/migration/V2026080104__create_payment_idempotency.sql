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
