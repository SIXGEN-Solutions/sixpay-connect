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
