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

-- Migrate the DA-3 Local authorization snapshot to the canonical SIXPAY user.
-- After this migration, authentication identities no longer own authorization.
INSERT INTO security_user_roles (user_id, role)
SELECT DISTINCT
    local_user.user_id,
    SUBSTRING(local_authority.authority FROM 6)
FROM security_local_user_authorities local_authority
JOIN security_local_users local_user
    ON local_user.id = local_authority.local_user_id
WHERE local_authority.authority LIKE 'ROLE_%'
  AND LENGTH(local_authority.authority) > 5;

INSERT INTO security_user_permissions (user_id, permission)
SELECT DISTINCT
    local_user.user_id,
    local_authority.authority
FROM security_local_user_authorities local_authority
JOIN security_local_users local_user
    ON local_user.id = local_authority.local_user_id
WHERE local_authority.authority NOT LIKE 'ROLE_%';

-- The table was a temporary DA-3 credential-owned authorization store.
-- Its contents have now been migrated to canonical SIXPAY user ownership.
DROP TABLE security_local_user_authorities;

COMMENT ON TABLE security_user_roles IS
    'SIXPAY-owned business roles. Authentication providers do not own these values.';

COMMENT ON TABLE security_user_permissions IS
    'SIXPAY-owned business permissions independent from Local or OIDC authentication.';
