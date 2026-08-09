CREATE TABLE IF NOT EXISTS sixpay.local_auth_user (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    subject VARCHAR(150) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_local_auth_user_username UNIQUE (username),
    CONSTRAINT uk_local_auth_user_subject UNIQUE (subject)
);

CREATE TABLE IF NOT EXISTS sixpay.local_auth_user_role (
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    CONSTRAINT pk_local_auth_user_role PRIMARY KEY (user_id, role),
    CONSTRAINT fk_local_auth_user_role_user
        FOREIGN KEY (user_id)
        REFERENCES sixpay.local_auth_user(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_local_auth_user_role
        CHECK (role IN ('ADMIN', 'MANAGER', 'AUDITOR', 'PARTNER'))
);

CREATE INDEX IF NOT EXISTS idx_local_auth_user_enabled
    ON sixpay.local_auth_user(enabled);
