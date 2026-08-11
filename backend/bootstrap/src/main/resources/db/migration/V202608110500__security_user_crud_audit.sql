ALTER TABLE security_audit_events
    DROP CONSTRAINT ck_security_audit_event_type;

ALTER TABLE security_audit_events
    ADD CONSTRAINT ck_security_audit_event_type CHECK (
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
            'USER_CREATED',
            'USER_UPDATED',
            'USER_ENABLED',
            'USER_DISABLED',
            'USER_DELETED'
        )
    );
