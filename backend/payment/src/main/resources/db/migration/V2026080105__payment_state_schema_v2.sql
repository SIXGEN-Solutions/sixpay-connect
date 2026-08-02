ALTER TABLE payments
DROP CONSTRAINT ck_payments_state_schema_version;

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_state_schema_version
        CHECK (
            state_payload ? 'schemaVersion'
    AND (
    state_payload ->> 'schemaVersion'
    )::INTEGER IN (1,2)
    );