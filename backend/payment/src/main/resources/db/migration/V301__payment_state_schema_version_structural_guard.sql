-- SIXPAY CONNECT: Payment state document structural version guard (option B).
-- Preserve Flyway V300 checksum. Flyway applies this corrective migration to
-- existing and fresh installations immediately after Payment baseline V300.
-- PaymentStateDocument.validateSchemaCompatibility() owns the supported-version
-- upper bound (currently 7) and version-specific domain compatibility rules.
-- The database only requires a positive JSON numeric integer version.
-- PostgreSQL DDL is transactional; no cleanup or data rewrite is performed.

ALTER TABLE payments
    DROP CONSTRAINT ck_payments_state_schema_version;

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_state_schema_version
    CHECK (
        state_payload ? 'schemaVersion'
        AND jsonb_typeof(state_payload -> 'schemaVersion') = 'number'
        AND (state_payload ->> 'schemaVersion') ~ '^[1-9][0-9]*$'
    );
