-- INIT-2A — Partner business identity.
-- Existing development Partner rows have no authoritative business identifier.
-- Recreate such development databases instead of inventing identifiers.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM partners LIMIT 1) THEN
        RAISE EXCEPTION
            'V101 requires recreation of development database: existing Partner rows have no authoritative partnerIdentifier';
    END IF;
END
$$;

ALTER TABLE partners
    ADD COLUMN partner_identifier VARCHAR(64) NOT NULL;

ALTER TABLE partners
    ADD CONSTRAINT uk_partners_partner_identifier
        UNIQUE (partner_identifier);
