-- SIXPAY CONNECT canonical pre-production Flyway baseline
-- FS-2.3 Database baseline consolidation
-- This file represents current schema state; Git preserves prior migration history.


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V202607272300__create_notification_deliveries.sql
-- ---------------------------------------------------------------------------

CREATE TABLE notification_deliveries (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    recipient VARCHAR(254) NOT NULL,
    template VARCHAR(100) NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    last_error VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    correlation_id VARCHAR(150) NOT NULL,

    CONSTRAINT uk_notification_deliveries_event
        UNIQUE (event_id),
    CONSTRAINT ck_notification_deliveries_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'SENT',
                'FAILED',
                'DEAD'
            )
        ),
    CONSTRAINT ck_notification_deliveries_attempt_count
        CHECK (attempt_count >= 0),
    CONSTRAINT ck_notification_deliveries_sent
        CHECK (
            status <> 'SENT'
            OR sent_at IS NOT NULL
        )
);

CREATE INDEX ix_notification_deliveries_retry
    ON notification_deliveries (status, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'FAILED', 'PROCESSING');

CREATE INDEX ix_notification_deliveries_aggregate
    ON notification_deliveries (aggregate_id, created_at);

CREATE INDEX ix_notification_deliveries_correlation
    ON notification_deliveries (correlation_id);


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V202607280100__add_notification_delivery_retry_payload.sql
-- ---------------------------------------------------------------------------

ALTER TABLE notification_deliveries
    ADD COLUMN reason VARCHAR(1000);


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V202608071300__operational_notifications.sql
-- ---------------------------------------------------------------------------

CREATE SCHEMA IF NOT EXISTS sixpay;

CREATE TABLE IF NOT EXISTS sixpay.operational_notification_deliveries (
    notification_id UUID PRIMARY KEY,
    trigger_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(128) NOT NULL,
    recipient_type VARCHAR(64) NOT NULL,
    recipient_reference VARCHAR(128) NOT NULL,
    recipient_locale VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    template_key VARCHAR(96) NOT NULL,
    deduplication_key VARCHAR(64) NOT NULL,
    template_variables TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    last_attempt_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    last_error_code VARCHAR(128),
    provider_reference VARCHAR(256),
    created_at TIMESTAMPTZ NOT NULL,
    correlation_id VARCHAR(150) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_operational_notification_deduplication
        UNIQUE (deduplication_key),

    CONSTRAINT ck_operational_notification_attempt_count
        CHECK (attempt_count >= 0),

    CONSTRAINT ck_operational_notification_status
        CHECK (
            status IN (
                'PENDING',
                'DISPATCHING',
                'ACCEPTED',
                'DELIVERED',
                'FAILED_RETRYABLE',
                'FAILED_PERMANENT',
                'DEAD_LETTERED'
            )
        )
);

CREATE TABLE IF NOT EXISTS sixpay.operational_notification_attempts (
    attempt_id UUID PRIMARY KEY,
    notification_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    outcome VARCHAR(32) NOT NULL,
    error_code VARCHAR(128),

    CONSTRAINT fk_operational_notification_attempt_delivery
        FOREIGN KEY (notification_id)
        REFERENCES sixpay.operational_notification_deliveries(
            notification_id
        )
        ON DELETE CASCADE,

    CONSTRAINT uk_operational_notification_attempt_number
        UNIQUE (
            notification_id,
            attempt_number
        ),

    CONSTRAINT ck_operational_notification_attempt_number
        CHECK (attempt_number > 0),

    CONSTRAINT ck_operational_notification_attempt_outcome
        CHECK (
            outcome IN (
                'STARTED',
                'ACCEPTED',
                'DELIVERED',
                'FAILED_RETRYABLE',
                'FAILED_PERMANENT'
            )
        )
);

CREATE INDEX IF NOT EXISTS idx_operational_notification_due
    ON sixpay.operational_notification_deliveries (
        status,
        next_attempt_at,
        created_at
    );

CREATE INDEX IF NOT EXISTS idx_operational_notification_source
    ON sixpay.operational_notification_deliveries (
        trigger_type,
        source_id
    );

CREATE INDEX IF NOT EXISTS idx_operational_notification_attempts_notification
    ON sixpay.operational_notification_attempts (
        notification_id,
        attempt_number
    );


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V202608071400__operational_notification_operations.sql
-- ---------------------------------------------------------------------------

ALTER TABLE sixpay.operational_notification_deliveries
    ADD COLUMN IF NOT EXISTS cycle_attempt_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE sixpay.operational_notification_deliveries
    ADD COLUMN IF NOT EXISTS replay_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE sixpay.operational_notification_deliveries
    ADD COLUMN IF NOT EXISTS last_replay_at TIMESTAMPTZ;

ALTER TABLE sixpay.operational_notification_deliveries
    ADD CONSTRAINT ck_operational_notification_cycle_attempt_count
        CHECK (cycle_attempt_count >= 0);

ALTER TABLE sixpay.operational_notification_deliveries
    ADD CONSTRAINT ck_operational_notification_replay_count
        CHECK (replay_count >= 0);

CREATE TABLE IF NOT EXISTS sixpay.operational_notification_replays (
    replay_id UUID PRIMARY KEY,
    notification_id UUID NOT NULL,
    operator_reference VARCHAR(128) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    previous_status VARCHAR(32) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_operational_notification_replay_delivery
        FOREIGN KEY (notification_id)
        REFERENCES sixpay.operational_notification_deliveries(
            notification_id
        )
        ON DELETE CASCADE,

    CONSTRAINT ck_operational_notification_replay_previous_status
        CHECK (previous_status = 'DEAD_LETTERED')
);

CREATE INDEX IF NOT EXISTS idx_operational_notification_replays_notification
    ON sixpay.operational_notification_replays (
        notification_id,
        requested_at
    );

CREATE INDEX IF NOT EXISTS idx_operational_notification_terminal_retention
    ON sixpay.operational_notification_deliveries (
        status,
        delivered_at,
        last_attempt_at,
        created_at
    );
