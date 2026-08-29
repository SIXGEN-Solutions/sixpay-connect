-- SIXPAY CONNECT canonical pre-production Flyway baseline
-- FS-2.3 Database baseline consolidation
-- This file represents current schema state; Git preserves prior migration history.

-- ---------------------------------------------------------------------------
-- Canonical SIXPAY users
-- ---------------------------------------------------------------------------

CREATE TABLE security_user_accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(150) NOT NULL,
    normalized_username VARCHAR(150) NOT NULL,
    email VARCHAR(320),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_security_user_accounts_username UNIQUE (username),
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

COMMENT ON TABLE security_user_accounts IS
    'Canonical SIXPAY users, independent from authentication mechanism.';

-- ---------------------------------------------------------------------------
-- Authentication identities
-- ---------------------------------------------------------------------------

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

COMMENT ON TABLE security_user_identities IS
    'Authentication identities linked to canonical SIXPAY users. No automatic email linking.';

COMMENT ON COLUMN security_user_identities.provider IS
    'LOCAL uses SIXPAY. OIDC uses the exact trusted issuer URI.';

-- ---------------------------------------------------------------------------
-- LOCAL credential store input final lifecycle shape
-- ---------------------------------------------------------------------------

CREATE TABLE security_local_users (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    subject VARCHAR(150) NOT NULL,
    username VARCHAR(150) NOT NULL,
    normalized_username VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_authenticated_at TIMESTAMPTZ,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    password_changed_at TIMESTAMPTZ,
    password_expires_at TIMESTAMPTZ,
    credential_updated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_security_local_user_account
        FOREIGN KEY (user_id)
        REFERENCES security_user_accounts (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_security_local_user_account UNIQUE (user_id),
    CONSTRAINT uk_security_local_users_subject UNIQUE (subject),
    CONSTRAINT uk_security_local_users_normalized_username
        UNIQUE (normalized_username),
    CONSTRAINT ck_security_local_users_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_security_local_users_failed_attempts
        CHECK (failed_attempts >= 0),
    CONSTRAINT ck_security_local_users_normalized_username
        CHECK (
            normalized_username = LOWER(BTRIM(normalized_username))
            AND NULLIF(BTRIM(normalized_username), '') IS NOT NULL
        ),
    CONSTRAINT ck_security_local_users_timestamps
        CHECK (updated_at >= created_at),
    CONSTRAINT ck_security_local_users_password_expiration
        CHECK (
            password_expires_at IS NULL
            OR (
                password_changed_at IS NOT NULL
                AND password_expires_at > password_changed_at
            )
        ),
    CONSTRAINT ck_security_local_users_temporary_password_expiration
        CHECK (
            NOT must_change_password
            OR password_expires_at IS NULL
        )
);

CREATE INDEX ix_security_local_users_status
    ON security_local_users (status);

CREATE INDEX ix_security_local_users_locked_until
    ON security_local_users (locked_until)
    WHERE locked_until IS NOT NULL;

CREATE INDEX ix_security_local_users_password_expires_at
    ON security_local_users (password_expires_at)
    WHERE password_expires_at IS NOT NULL;

COMMENT ON COLUMN security_local_users.must_change_password IS
    'True for administratively provisioned/reset LOCAL credentials until the user changes the password.';
COMMENT ON COLUMN security_local_users.password_changed_at IS
    'Timestamp of the last user-owned password change. NULL for temporary credentials.';
COMMENT ON COLUMN security_local_users.password_expires_at IS
    'Configured normal credential expiration. NULL for temporary credentials.';
COMMENT ON COLUMN security_local_users.credential_updated_at IS
    'Timestamp of the last credential material/lifecycle update; authentication-state updates do not change it.';

-- ---------------------------------------------------------------------------
-- SIXPAY-owned authorization
-- ---------------------------------------------------------------------------

CREATE TABLE security_user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_security_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES security_user_accounts (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_security_user_roles_non_blank
        CHECK (NULLIF(BTRIM(role), '') IS NOT NULL),
    CONSTRAINT ck_security_user_roles_without_prefix
        CHECK (role NOT LIKE 'ROLE_%')
);

CREATE TABLE security_user_permissions (
    user_id UUID NOT NULL,
    permission VARCHAR(150) NOT NULL,
    PRIMARY KEY (user_id, permission),
    CONSTRAINT fk_security_user_permissions_user
        FOREIGN KEY (user_id)
        REFERENCES security_user_accounts (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_security_user_permissions_non_blank
        CHECK (NULLIF(BTRIM(permission), '') IS NOT NULL)
);

COMMENT ON TABLE security_user_roles IS
    'SIXPAY-owned business roles. Authentication providers do not own these values.';

COMMENT ON TABLE security_user_permissions IS
    'SIXPAY-owned business permissions independent from Local or OIDC authentication.';

-- ---------------------------------------------------------------------------
-- Password history
-- ---------------------------------------------------------------------------

CREATE TABLE security_password_history (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_security_password_history_user
        FOREIGN KEY (user_id)
        REFERENCES security_user_accounts (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_security_password_history_hash
        CHECK (NULLIF(BTRIM(password_hash), '') IS NOT NULL)
);

CREATE INDEX ix_security_password_history_user_created
    ON security_password_history (user_id, created_at DESC);

COMMENT ON TABLE security_password_history IS
    'Recent one-way LOCAL password hashes retained only for password anti-reuse validation.';

-- ---------------------------------------------------------------------------
-- Authentication audit
-- ---------------------------------------------------------------------------

CREATE TABLE security_authentication_audit (
    id UUID PRIMARY KEY,
    event_type VARCHAR(32) NOT NULL,
    subject VARCHAR(150),
    username VARCHAR(150) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_security_auth_audit_type
        CHECK (event_type IN ('LOGIN', 'LOGOUT')),
    CONSTRAINT ck_security_auth_audit_outcome
        CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT ck_security_auth_audit_username
        CHECK (NULLIF(BTRIM(username), '') IS NOT NULL)
);

CREATE INDEX ix_security_auth_audit_subject_time
    ON security_authentication_audit (subject, occurred_at);

CREATE INDEX ix_security_auth_audit_username_time
    ON security_authentication_audit (username, occurred_at);

CREATE OR REPLACE FUNCTION security_reject_auth_audit_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'table % is append-only', TG_TABLE_NAME
        USING ERRCODE = '55000';
END;
$$;

CREATE TRIGGER trg_security_auth_audit_immutable
    BEFORE UPDATE OR DELETE ON security_authentication_audit
    FOR EACH ROW EXECUTE FUNCTION security_reject_auth_audit_change();

-- ---------------------------------------------------------------------------
-- Operational security audit input final event-type shape
-- ---------------------------------------------------------------------------

CREATE TABLE security_audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    actor_subject VARCHAR(150),
    target_user_id UUID,
    username VARCHAR(150),
    provider VARCHAR(500),
    detail VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_security_audit_event_type CHECK (
        event_type IN (
            'LOGIN_SUCCESS',
            'LOGIN_FAILURE',
            'LOGOUT',
            'PASSWORD_CHANGED',
            'PASSWORD_RESET',
            'ACCOUNT_LOCKED',
            'OIDC_LOGIN_SUCCESS',
            'OIDC_LOGIN_FAILURE',
            'IDENTITY_LINKED',
            'IDENTITY_UNLINKED',
            'AUTH_METHOD_ENABLED',
            'AUTH_METHOD_DISABLED',
            'USER_CREATED',
            'USER_UPDATED',
            'USER_ENABLED',
            'USER_DISABLED',
            'USER_DELETED'
        )
    ),
    CONSTRAINT fk_security_audit_target_user
        FOREIGN KEY (target_user_id)
        REFERENCES security_user_accounts (id)
        ON DELETE SET NULL
);

CREATE INDEX ix_security_audit_target_time
    ON security_audit_events (target_user_id, occurred_at DESC);

CREATE INDEX ix_security_audit_event_time
    ON security_audit_events (event_type, occurred_at DESC);

CREATE OR REPLACE FUNCTION security_reject_operational_audit_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'table % is append-only', TG_TABLE_NAME
        USING ERRCODE = '55000';
END;
$$;

CREATE TRIGGER trg_security_operational_audit_immutable
    BEFORE UPDATE OR DELETE ON security_audit_events
    FOR EACH ROW EXECUTE FUNCTION security_reject_operational_audit_change();

COMMENT ON TABLE security_audit_events IS
    'Append-only SIXPAY authentication and security-administration audit. Secrets and tokens are forbidden.';

COMMENT ON CONSTRAINT ck_security_audit_event_type
    ON security_audit_events IS
    'Allowed values synchronized with SecurityAuditEventType.';
