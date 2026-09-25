-- INIT-2B — Partner M2M machine identity link.
-- This table belongs to Security: it binds a trusted technical caller identity
-- to the Partner business identifier that Partner owns.

CREATE TABLE security_partner_machine_identities (
    id UUID PRIMARY KEY,
    machine_subject VARCHAR(255) NOT NULL,
    partner_identifier VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_security_partner_machine_identity_subject
        UNIQUE (machine_subject),
    CONSTRAINT ck_security_partner_machine_identity_subject
        CHECK (NULLIF(BTRIM(machine_subject), '') IS NOT NULL),
    CONSTRAINT ck_security_partner_machine_identity_partner
        CHECK (NULLIF(BTRIM(partner_identifier), '') IS NOT NULL),
    CONSTRAINT ck_security_partner_machine_identity_timestamps
        CHECK (updated_at >= created_at)
);

CREATE INDEX ix_security_partner_machine_identity_partner
    ON security_partner_machine_identities (partner_identifier)
    WHERE enabled = TRUE;

COMMENT ON TABLE security_partner_machine_identities IS
    'Security-owned binding from authenticated Partner M2M machine subject to canonical Partner business identifier.';
