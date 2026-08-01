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
