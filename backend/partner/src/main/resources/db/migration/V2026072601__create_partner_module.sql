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
    currency CHAR(3) NOT NULL,
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
    currency CHAR(3) NOT NULL,
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
