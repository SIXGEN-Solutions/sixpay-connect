-- R3 operator audit is independent of immutable Payment domain event audit.
CREATE TABLE payment_t0_manual_reconciliation_audit (
    event_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    actor_subject TEXT NOT NULL,
    correlation_id UUID NOT NULL,
    outcome VARCHAR(48) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_payment_t0_manual_audit_actor CHECK (btrim(actor_subject) <> ''),
    CONSTRAINT ck_payment_t0_manual_audit_outcome CHECK (outcome IN (
        'DENIED', 'NOT_ELIGIBLE_OR_BUSY', 'RETRY_SCHEDULED', 'RESOLVED',
        'STALE_CLAIM', 'LOOKUP_FAILED_RETRY_SCHEDULED'
    ))
);
CREATE INDEX idx_payment_t0_manual_audit_payment_time
    ON payment_t0_manual_reconciliation_audit (payment_id, occurred_at);
CREATE INDEX idx_payment_t0_manual_audit_correlation
    ON payment_t0_manual_reconciliation_audit (correlation_id);
CREATE OR REPLACE FUNCTION reject_payment_t0_manual_audit_mutation()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'payment_t0_manual_reconciliation_audit is append-only';
END;
$$;
CREATE TRIGGER trg_payment_t0_manual_audit_reject_update
    BEFORE UPDATE ON payment_t0_manual_reconciliation_audit
    FOR EACH ROW EXECUTE FUNCTION reject_payment_t0_manual_audit_mutation();
CREATE TRIGGER trg_payment_t0_manual_audit_reject_delete
    BEFORE DELETE ON payment_t0_manual_reconciliation_audit
    FOR EACH ROW EXECUTE FUNCTION reject_payment_t0_manual_audit_mutation();
