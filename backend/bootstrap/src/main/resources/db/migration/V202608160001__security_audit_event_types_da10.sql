ALTER TABLE security_audit_events
    DROP CONSTRAINT IF EXISTS ck_security_audit_event_type;

ALTER TABLE security_audit_events
    ADD CONSTRAINT ck_security_audit_event_type
        CHECK (
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
        );

COMMENT ON CONSTRAINT ck_security_audit_event_type
    ON security_audit_events IS
    'Allowed values synchronized with SecurityAuditEventType.';
