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
