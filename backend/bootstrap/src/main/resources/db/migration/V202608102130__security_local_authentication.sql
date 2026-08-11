CREATE TABLE security_local_users (
    id UUID PRIMARY KEY,
    subject VARCHAR(150) NOT NULL,
    username VARCHAR(150) NOT NULL,
    normalized_username VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_authenticated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_security_local_users_subject UNIQUE (subject),
    CONSTRAINT uk_security_local_users_normalized_username UNIQUE (normalized_username),
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
        CHECK (updated_at >= created_at)
);

CREATE INDEX ix_security_local_users_status
    ON security_local_users (status);

CREATE INDEX ix_security_local_users_locked_until
    ON security_local_users (locked_until)
    WHERE locked_until IS NOT NULL;

CREATE TABLE security_local_user_authorities (
    local_user_id UUID NOT NULL,
    authority VARCHAR(150) NOT NULL,

    PRIMARY KEY (local_user_id, authority),

    CONSTRAINT fk_security_local_authorities_user
        FOREIGN KEY (local_user_id)
        REFERENCES security_local_users (id)
        ON DELETE CASCADE,

    CONSTRAINT ck_security_local_authority_non_blank
        CHECK (NULLIF(BTRIM(authority), '') IS NOT NULL)
);


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
