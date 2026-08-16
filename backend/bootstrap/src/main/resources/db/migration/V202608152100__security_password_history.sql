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
