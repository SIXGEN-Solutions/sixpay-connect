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
            'PASSWORD_RESET',
            'ACCOUNT_LOCKED',
            'OIDC_LOGIN_SUCCESS',
            'OIDC_LOGIN_FAILURE',
            'IDENTITY_LINKED',
            'IDENTITY_UNLINKED',
            'AUTH_METHOD_ENABLED',
            'AUTH_METHOD_DISABLED',
            'USER_DISABLED'
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
