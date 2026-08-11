CREATE TABLE security_user_accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(150) NOT NULL,
    normalized_username VARCHAR(150) NOT NULL,
    email VARCHAR(320),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_security_user_accounts_username
        UNIQUE (username),
    CONSTRAINT uk_security_user_accounts_normalized_username
        UNIQUE (normalized_username),
    CONSTRAINT ck_security_user_accounts_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_security_user_accounts_normalized_username
        CHECK (
            normalized_username = LOWER(BTRIM(normalized_username))
            AND NULLIF(BTRIM(normalized_username), '') IS NOT NULL
        ),
    CONSTRAINT ck_security_user_accounts_timestamps
        CHECK (updated_at >= created_at)
);

CREATE UNIQUE INDEX uk_security_user_accounts_email_ci
    ON security_user_accounts (LOWER(email))
    WHERE email IS NOT NULL;

INSERT INTO security_user_accounts (
    id,
    username,
    normalized_username,
    email,
    status,
    created_at,
    updated_at,
    version
)
SELECT
    local_user.id,
    local_user.username,
    local_user.normalized_username,
    NULL,
    local_user.status,
    local_user.created_at,
    local_user.updated_at,
    0
FROM security_local_users local_user;

CREATE TABLE security_user_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    identity_type VARCHAR(32) NOT NULL,
    provider VARCHAR(500) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_security_user_identity_user
        FOREIGN KEY (user_id)
        REFERENCES security_user_accounts (id)
        ON DELETE CASCADE,

    CONSTRAINT ck_security_user_identity_type
        CHECK (identity_type IN ('LOCAL', 'OIDC')),

    CONSTRAINT ck_security_user_identity_provider
        CHECK (NULLIF(BTRIM(provider), '') IS NOT NULL),

    CONSTRAINT ck_security_user_identity_subject
        CHECK (NULLIF(BTRIM(provider_subject), '') IS NOT NULL),

    CONSTRAINT ck_security_user_identity_timestamps
        CHECK (updated_at >= created_at),

    CONSTRAINT uk_security_user_identity_provider_subject
        UNIQUE (identity_type, provider, provider_subject),

    CONSTRAINT uk_security_user_identity_user_provider
        UNIQUE (user_id, identity_type, provider)
);

CREATE INDEX ix_security_user_identities_user
    ON security_user_identities (user_id);

INSERT INTO security_user_identities (
    id,
    user_id,
    identity_type,
    provider,
    provider_subject,
    created_at,
    updated_at
)
SELECT
    local_user.id,
    local_user.id,
    'LOCAL',
    'SIXPAY',
    local_user.subject,
    local_user.created_at,
    local_user.updated_at
FROM security_local_users local_user;

ALTER TABLE security_local_users
    ADD COLUMN user_id UUID;

UPDATE security_local_users
SET user_id = id;

ALTER TABLE security_local_users
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE security_local_users
    ADD CONSTRAINT fk_security_local_user_account
        FOREIGN KEY (user_id)
        REFERENCES security_user_accounts (id),
    ADD CONSTRAINT uk_security_local_user_account
        UNIQUE (user_id);

COMMENT ON TABLE security_user_accounts IS
    'Canonical SIXPAY users, independent from authentication mechanism.';

COMMENT ON TABLE security_user_identities IS
    'Authentication identities linked to canonical SIXPAY users. No automatic email linking.';

COMMENT ON COLUMN security_user_identities.provider IS
    'LOCAL uses SIXPAY. OIDC uses the exact trusted issuer URI.';
