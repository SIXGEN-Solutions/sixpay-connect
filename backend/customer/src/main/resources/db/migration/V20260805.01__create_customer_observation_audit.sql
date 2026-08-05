CREATE TABLE customer_observation_audit (
    audit_id UUID PRIMARY KEY,
    action VARCHAR(64) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    observed_customer_id UUID NULL,
    payment_id UUID NULL,
    source_event_id UUID NULL,
    actor_id VARCHAR(150) NOT NULL,
    correlation_id VARCHAR(150) NOT NULL,
    reason_code VARCHAR(100) NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    audit_version INTEGER NOT NULL,

    CONSTRAINT ck_customer_observation_audit_version
        CHECK (audit_version > 0),

    CONSTRAINT ck_customer_observation_audit_actor
        CHECK (length(btrim(actor_id)) > 0),

    CONSTRAINT ck_customer_observation_audit_correlation
        CHECK (length(btrim(correlation_id)) > 0),

    CONSTRAINT ck_customer_observation_audit_reason
        CHECK (
            reason_code IS NULL
            OR reason_code ~ '^[A-Z0-9][A-Z0-9_.-]*$'
        )
);

CREATE INDEX idx_customer_observation_audit_customer
    ON customer_observation_audit (
        observed_customer_id,
        occurred_at
    );

CREATE INDEX idx_customer_observation_audit_source_event
    ON customer_observation_audit (
        source_event_id
    );

CREATE INDEX idx_customer_observation_audit_correlation
    ON customer_observation_audit (
        correlation_id,
        occurred_at
    );

CREATE INDEX idx_customer_observation_audit_occurred
    ON customer_observation_audit (
        occurred_at
    );

CREATE OR REPLACE FUNCTION reject_customer_observation_audit_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'customer_observation_audit is append-only';
END;
$$;

CREATE TRIGGER trg_customer_observation_audit_no_update
BEFORE UPDATE ON customer_observation_audit
FOR EACH ROW
EXECUTE FUNCTION reject_customer_observation_audit_mutation();

CREATE TRIGGER trg_customer_observation_audit_no_delete
BEFORE DELETE ON customer_observation_audit
FOR EACH ROW
EXECUTE FUNCTION reject_customer_observation_audit_mutation();
