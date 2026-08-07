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
