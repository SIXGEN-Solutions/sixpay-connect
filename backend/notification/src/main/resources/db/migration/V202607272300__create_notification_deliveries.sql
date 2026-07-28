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
