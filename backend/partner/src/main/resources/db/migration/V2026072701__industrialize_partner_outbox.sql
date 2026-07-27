ALTER TABLE partner_outbox_events
    ADD COLUMN schema_version INTEGER,
    ADD COLUMN correlation_id VARCHAR(150),
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMPTZ,
    ADD COLUMN last_attempt_at TIMESTAMPTZ,
    ADD COLUMN claimed_at TIMESTAMPTZ,
    ADD COLUMN claimed_by VARCHAR(100);

UPDATE partner_outbox_events
   SET schema_version = (payload ->> 'schemaVersion')::INTEGER,
       correlation_id = payload ->> 'correlationId',
       next_attempt_at = created_at;

ALTER TABLE partner_outbox_events
    ALTER COLUMN schema_version SET NOT NULL,
    ALTER COLUMN correlation_id SET NOT NULL,
    ALTER COLUMN next_attempt_at SET NOT NULL;

ALTER TABLE partner_outbox_events
    DROP CONSTRAINT ck_partner_outbox_status;

ALTER TABLE partner_outbox_events
    ADD CONSTRAINT ck_partner_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED', 'DEAD')),
    ADD CONSTRAINT ck_partner_outbox_schema_version
        CHECK (schema_version > 0),
    ADD CONSTRAINT ck_partner_outbox_attempt_count
        CHECK (attempt_count >= 0);

DROP INDEX ix_partner_outbox_pending;

CREATE INDEX ix_partner_outbox_claimable
    ON partner_outbox_events (status, next_attempt_at, occurred_at)
    WHERE status IN ('PENDING', 'FAILED', 'PROCESSING');
