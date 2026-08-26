-- SIXPAY CONNECT canonical pre-production Flyway baseline
-- FS-2.3 Database baseline consolidation
-- This file represents current schema state; Git preserves prior migration history.


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V2026072601__create_partner_module.sql
-- ---------------------------------------------------------------------------

CREATE TABLE partners (
    id UUID PRIMARY KEY,
    legal_name VARCHAR(200) NOT NULL,
    technical_contact_name VARCHAR(150) NOT NULL,
    technical_contact_email VARCHAR(254) NOT NULL,
    status VARCHAR(32) NOT NULL,
    status_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_partners_status
        CHECK (status IN ('PENDING_VALIDATION', 'ACTIVE', 'REJECTED', 'SUSPENDED')),
    CONSTRAINT ck_partners_rejection_reason
        CHECK (status <> 'REJECTED' OR NULLIF(BTRIM(status_reason), '') IS NOT NULL),
    CONSTRAINT ck_partners_suspension_reason
        CHECK (status <> 'SUSPENDED' OR NULLIF(BTRIM(status_reason), '') IS NOT NULL),
    CONSTRAINT ck_partners_created_updated
        CHECK (updated_at >= created_at)
);

CREATE INDEX ix_partners_status ON partners (status);
CREATE INDEX ix_partners_technical_contact_email ON partners (LOWER(technical_contact_email));

CREATE TABLE partner_authorized_perimeters (
    partner_id UUID NOT NULL,
    transaction_type VARCHAR(64) NOT NULL,
    PRIMARY KEY (partner_id, transaction_type),
    CONSTRAINT fk_partner_authorized_perimeters_partner
        FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE CASCADE,
    CONSTRAINT ck_partner_authorized_perimeters_type
        CHECK (transaction_type = UPPER(BTRIM(transaction_type)))
);

CREATE TABLE partner_validation_thresholds (
    partner_id UUID NOT NULL,
    transaction_type VARCHAR(64) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    threshold_amount NUMERIC(19, 4) NOT NULL,
    validation_levels INTEGER NOT NULL,
    PRIMARY KEY (partner_id, transaction_type, currency),
    CONSTRAINT fk_partner_validation_thresholds_partner
        FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_validation_thresholds_perimeter
        FOREIGN KEY (partner_id, transaction_type)
        REFERENCES partner_authorized_perimeters (partner_id, transaction_type)
        ON DELETE CASCADE,
    CONSTRAINT ck_partner_validation_thresholds_amount
        CHECK (threshold_amount > 0),
    CONSTRAINT ck_partner_validation_thresholds_levels
        CHECK (validation_levels BETWEEN 1 AND 10),
    CONSTRAINT ck_partner_validation_thresholds_type
        CHECK (transaction_type = UPPER(BTRIM(transaction_type))),
    CONSTRAINT ck_partner_validation_thresholds_currency
        CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE TABLE partner_validation_threshold_history (
    id UUID PRIMARY KEY,
    partner_id UUID NOT NULL,
    transaction_type VARCHAR(64) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    previous_amount NUMERIC(19, 4),
    previous_validation_levels INTEGER,
    current_amount NUMERIC(19, 4) NOT NULL,
    current_validation_levels INTEGER NOT NULL,
    actor_id VARCHAR(150) NOT NULL,
    correlation_id VARCHAR(150) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_partner_threshold_history_partner
        FOREIGN KEY (partner_id) REFERENCES partners (id),
    CONSTRAINT ck_partner_threshold_history_previous
        CHECK (
            (previous_amount IS NULL AND previous_validation_levels IS NULL)
            OR
            (
                previous_amount IS NOT NULL
                AND previous_validation_levels IS NOT NULL
                AND previous_amount > 0
                AND previous_validation_levels BETWEEN 1 AND 10
            )
        ),
    CONSTRAINT ck_partner_threshold_history_current
        CHECK (current_amount > 0 AND current_validation_levels BETWEEN 1 AND 10),
    CONSTRAINT ck_partner_threshold_history_currency
        CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX ix_partner_threshold_history_partner_period
    ON partner_validation_threshold_history (partner_id, changed_at);

CREATE TABLE partner_audit (
    id UUID PRIMARY KEY,
    partner_id UUID NOT NULL,
    action VARCHAR(64) NOT NULL,
    result VARCHAR(32) NOT NULL,
    actor_id VARCHAR(150) NOT NULL,
    correlation_id VARCHAR(150) NOT NULL,
    details VARCHAR(1000) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_partner_audit_partner
        FOREIGN KEY (partner_id) REFERENCES partners (id)
);

CREATE INDEX ix_partner_audit_partner_period
    ON partner_audit (partner_id, occurred_at);
CREATE INDEX ix_partner_audit_correlation
    ON partner_audit (correlation_id);

CREATE OR REPLACE FUNCTION partner_reject_immutable_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'table % is append-only', TG_TABLE_NAME
        USING ERRCODE = '55000';
END;
$$;

CREATE TRIGGER trg_partner_audit_immutable
    BEFORE UPDATE OR DELETE ON partner_audit
    FOR EACH ROW EXECUTE FUNCTION partner_reject_immutable_change();

CREATE TRIGGER trg_partner_threshold_history_immutable
    BEFORE UPDATE OR DELETE ON partner_validation_threshold_history
    FOR EACH ROW EXECUTE FUNCTION partner_reject_immutable_change();

CREATE TABLE partner_idempotency (
    id UUID PRIMARY KEY,
    operation VARCHAR(160) NOT NULL,
    idempotency_key VARCHAR(150) NOT NULL,
    partner_id UUID NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_partner_idempotency_operation_key
        UNIQUE (operation, idempotency_key),
    CONSTRAINT fk_partner_idempotency_partner
        FOREIGN KEY (partner_id) REFERENCES partners (id)
);

CREATE INDEX ix_partner_idempotency_completed_at
    ON partner_idempotency (completed_at);

CREATE TABLE partner_outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    failure_reason VARCHAR(1000),
    CONSTRAINT ck_partner_outbox_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_partner_outbox_aggregate
        CHECK (aggregate_type = 'PARTNER')
);

CREATE INDEX ix_partner_outbox_pending
    ON partner_outbox_events (status, occurred_at)
    WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX ix_partner_outbox_aggregate
    ON partner_outbox_events (aggregate_id, occurred_at);


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V2026072701__industrialize_partner_outbox.sql
-- ---------------------------------------------------------------------------

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
