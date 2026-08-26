-- SIXPAY CONNECT canonical pre-production Flyway baseline
-- FS-2.3 Database baseline consolidation
-- This file represents current schema state; Git preserves prior migration history.


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V202608072058__create_reporting_payment_audit_projection.sql
-- ---------------------------------------------------------------------------

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


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V202608072120__create_reporting_audit_export.sql
-- ---------------------------------------------------------------------------

ALTER TABLE reporting_payment_audit_evidence
    ADD COLUMN financial_institution_code VARCHAR(64) NULL;

CREATE INDEX idx_reporting_audit_institution
    ON reporting_payment_audit_evidence (
        financial_institution_code,
        occurred_at DESC
    );

CREATE TABLE reporting_payment_audit_export_job (
    export_id UUID PRIMARY KEY,
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    request_fingerprint VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    occurred_from TIMESTAMPTZ NOT NULL,
    occurred_to TIMESTAMPTZ NOT NULL,
    payment_ids TEXT NOT NULL DEFAULT '',
    financial_institution_codes TEXT NOT NULL DEFAULT '',
    actions TEXT NOT NULL DEFAULT '',
    results TEXT NOT NULL DEFAULT '',
    business_purpose VARCHAR(500) NOT NULL,
    export_format VARCHAR(10) NOT NULL,
    requested_by VARCHAR(128) NOT NULL,
    correlation_id UUID NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    generation_started_at TIMESTAMPTZ NULL,
    completed_at TIMESTAMPTZ NULL,
    record_count BIGINT NULL,
    checksum VARCHAR(128) NULL,
    retrieval_uri VARCHAR(2048) NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    failure_code VARCHAR(100) NULL
);

CREATE INDEX idx_reporting_export_status_requested
    ON reporting_payment_audit_export_job (
        status,
        requested_at ASC
    );

CREATE INDEX idx_reporting_export_expiry
    ON reporting_payment_audit_export_job (
        status,
        expires_at
    );
