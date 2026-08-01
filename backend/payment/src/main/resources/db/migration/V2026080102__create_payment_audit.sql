CREATE TABLE payment_audit
(
    event_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    public_payment_reference VARCHAR(30) NOT NULL,
    event_type VARCHAR(96) NOT NULL,
    payment_status VARCHAR(48) NOT NULL,
    business_version BIGINT NOT NULL,
    event_sequence INTEGER NOT NULL,
    correlation_id VARCHAR(150) NOT NULL,
    causation_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_payment_audit PRIMARY KEY (event_id),
    CONSTRAINT fk_payment_audit_payment
        FOREIGN KEY (payment_id) REFERENCES payments (payment_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uk_payment_audit_payment_version_sequence
        UNIQUE (payment_id, business_version, event_sequence),
    CONSTRAINT ck_payment_audit_public_reference
        CHECK (public_payment_reference ~ '^PAY-[0-9A-HJKMNP-TV-Z]{26}$'),
    CONSTRAINT ck_payment_audit_business_version_positive CHECK (business_version > 0),
    CONSTRAINT ck_payment_audit_event_sequence_positive CHECK (event_sequence > 0),
    CONSTRAINT ck_payment_audit_event_type_not_blank CHECK (btrim(event_type) <> ''),
    CONSTRAINT ck_payment_audit_correlation_not_blank CHECK (btrim(correlation_id) <> '')
);

CREATE INDEX idx_payment_audit_payment_occurred_at
    ON payment_audit (payment_id, occurred_at);
CREATE INDEX idx_payment_audit_correlation_id
    ON payment_audit (correlation_id);
CREATE INDEX idx_payment_audit_event_type
    ON payment_audit (event_type);

CREATE OR REPLACE FUNCTION reject_payment_audit_mutation()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'payment_audit is append-only; % is forbidden', TG_OP;
END;
$$;

CREATE TRIGGER trg_payment_audit_reject_update
    BEFORE UPDATE ON payment_audit
    FOR EACH ROW EXECUTE FUNCTION reject_payment_audit_mutation();

CREATE TRIGGER trg_payment_audit_reject_delete
    BEFORE DELETE ON payment_audit
    FOR EACH ROW EXECUTE FUNCTION reject_payment_audit_mutation();
