ALTER TABLE security_local_users
    ADD COLUMN must_change_password BOOLEAN,
    ADD COLUMN password_changed_at TIMESTAMPTZ,
    ADD COLUMN password_expires_at TIMESTAMPTZ,
    ADD COLUMN credential_updated_at TIMESTAMPTZ;

-- Preserve existing pre-DA-10 credentials on upgrade. They become a legacy
-- lifecycle baseline and are not forced to change immediately.
UPDATE security_local_users
SET must_change_password = FALSE,
    password_changed_at = updated_at,
    password_expires_at = NULL,
    credential_updated_at = updated_at;

ALTER TABLE security_local_users
    ALTER COLUMN must_change_password SET NOT NULL,
    ALTER COLUMN must_change_password SET DEFAULT TRUE,
    ALTER COLUMN credential_updated_at SET NOT NULL;

ALTER TABLE security_local_users
    ADD CONSTRAINT ck_security_local_users_password_expiration
        CHECK (
            password_expires_at IS NULL
            OR (
                password_changed_at IS NOT NULL
                AND password_expires_at > password_changed_at
            )
        ),
    ADD CONSTRAINT ck_security_local_users_temporary_password_expiration
        CHECK (
            NOT must_change_password
            OR password_expires_at IS NULL
        );

CREATE INDEX ix_security_local_users_password_expires_at
    ON security_local_users (password_expires_at)
    WHERE password_expires_at IS NOT NULL;

COMMENT ON COLUMN security_local_users.must_change_password IS
    'True for administratively provisioned/reset LOCAL credentials until the user changes the password.';
COMMENT ON COLUMN security_local_users.password_changed_at IS
    'Timestamp of the last user-owned password change. NULL for temporary credentials.';
COMMENT ON COLUMN security_local_users.password_expires_at IS
    'Configured normal credential expiration. NULL for temporary or migrated legacy credentials.';
COMMENT ON COLUMN security_local_users.credential_updated_at IS
    'Timestamp of the last credential material/lifecycle update; authentication-state updates do not change it.';
