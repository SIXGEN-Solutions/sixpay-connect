CREATE TABLE reporting_payment_audit_evidence (
    evidence_id UUID PRIMARY KEY,
    timeline_visible BOOLEAN NOT NULL DEFAULT TRUE,
    audit_visible BOOLEAN NOT NULL DEFAULT TRUE,
    payment_id UUID NULL,
    payment_reference VARCHAR(64) NULL,
    observed_customer_id UUID NULL,
    category VARCHAR(40) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    timeline_result VARCHAR(20) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    actor_roles VARCHAR(1000) NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(150) NOT NULL,
    audit_result VARCHAR(20) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    correlation_id UUID NOT NULL,
    trace_id VARCHAR(128) NULL,
    source_system VARCHAR(32) NOT NULL,
    external_reference VARCHAR(150) NULL,
    before_state VARCHAR(64) NULL,
    after_state VARCHAR(64) NULL,
    aggregate_version BIGINT NOT NULL DEFAULT 0,
    integrity_scheme VARCHAR(32) NOT NULL,
    integrity_value VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reporting_audit_payment_timeline
    ON reporting_payment_audit_evidence (
        payment_id,
        occurred_at DESC,
        evidence_id DESC
    );

CREATE INDEX idx_reporting_audit_search
    ON reporting_payment_audit_evidence (
        occurred_at DESC,
        evidence_id DESC
    );

CREATE INDEX idx_reporting_audit_customer
    ON reporting_payment_audit_evidence (
        observed_customer_id,
        occurred_at DESC
    );

CREATE INDEX idx_reporting_audit_correlation
    ON reporting_payment_audit_evidence (
        correlation_id,
        occurred_at DESC
    );
