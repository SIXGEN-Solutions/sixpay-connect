CREATE TABLE customer_management_audit (
    audit_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    result VARCHAR(32) NOT NULL,
    actor_id VARCHAR(200) NOT NULL,
    correlation_id VARCHAR(150) NOT NULL,
    details VARCHAR(2000) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_customer_management_audit_result
        CHECK (result IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX ix_customer_management_audit_aggregate
    ON customer_management_audit (
        aggregate_type,
        aggregate_id,
        occurred_at
    );

CREATE INDEX ix_customer_management_audit_actor
    ON customer_management_audit (
        actor_id,
        occurred_at
    );

CREATE INDEX ix_customer_management_audit_correlation
    ON customer_management_audit (
        correlation_id
    );
