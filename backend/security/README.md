# Security Module

## Purpose

The Security module provides shared authentication, authorization, identity
linking, password lifecycle and security-audit capabilities.

## Capabilities

- local authentication and session management;
- OIDC session integration;
- Microsoft Active Directory authentication provider baseline over LDAPS;
- JWT resource-server authority conversion;
- SIXPAY-owned roles and permissions;
- local password change and reset support;
- user-account and external-identity linking;
- authentication and security operational audit.

The identity provider proves identity. SIXPAY owns authorization and maps the
authenticated identity to SIXPAY roles and permissions.

## API

Authentication and session endpoints:

    /api/v1/auth/login
    /api/v1/auth/login/ldap
    /api/v1/auth/session/oidc
    /api/v1/auth/me
    /api/v1/auth/logout
    /api/v1/auth/password/change

LOCAL, OIDC and LDAP converge on the same canonical SIXPAY principal and
backend session. LDAP credentials are transient and directory-owned; SIXPAY
does not persist them and never derives application authorities from LDAP
groups.

Administration exposes user-management HTTP boundaries while Security owns the
underlying users, identities, credentials and authorization data.

## Boundaries

- Security does not own business-domain aggregates.
- Administration calls Security application capabilities through ports.
- Business modules consume the authenticated principal and authorities.
- Secrets and provider credentials are supplied by runtime configuration.

## Validation

From backend:

    mvn -pl security -am test
    mvn -pl security -am clean verify
    mvn -pl security -am -Pfull-tests clean verify

The full-tests command requires Docker when integration tests are selected.

## Persistence ownership

Security owns these production tables:

| Table/family | Purpose |
|---|---|
| security_user_accounts | Canonical SIXPAY accounts |
| security_user_identities | Local and external identity links |
| security_local_users | Local credentials and state |
| security_user_roles | Role assignments |
| security_user_permissions | Permission assignments |
| security_password_history | Password history |
| security_authentication_audit | Authentication audit |
| security_audit_events | Security and authorization audit |

Administration exposes management HTTP boundaries but does not own these tables.

Schema:
backend/security/src/main/resources/db/migration/V700__security_baseline.sql


## Partner M2M machine identity

Partner system callers are not represented by `AuthenticatedUser`. Security
exposes `CurrentMachineIdentityProvider` for trusted technical subjects and owns
the durable `security_partner_machine_identities` association to a Partner
business identifier. Partner status and Partner business identity remain owned
by Partner.


## LDAP / Active Directory

Security owns LDAP/Active Directory authentication semantics, directory
discovery, stable identity mapping and the application-level provisioning
operation used by Administration.

The authentication path is:

```text
transient credentials
    -> AD authentication
    -> immutable LDAP identity (trust-domain + objectGUID)
    -> canonical SIXPAY account
    -> SIXPAY-owned roles / permissions
    -> canonical SIXPAY session
```

The administrative provisioning path is distinct:

```text
ADMIN exact directory lookup
    -> read-only directory projection
    -> explicit SIXPAY roles / permissions
    -> server-side directory revalidation
    -> canonical SIXPAY account with LOCAL authentication disabled
    -> LDAP identity link
    -> Security audit
```

Only a normalized `ACTIVE` directory account is provisionable. Existing LDAP
identity links and existing SIXPAY usernames are explicit conflicts; an
existing SIXPAY account is never auto-linked by the provisioning operation.

For Active Directory, the durable external identity is the configured trust
domain plus immutable `objectGUID`. Username, DN, email, display name and LDAP
groups are not durable linking keys. LDAP groups never become SIXPAY roles or
permissions.

Administration exposes the HTTP boundary, but it consumes only Security public
application surfaces. LDAP implementation classes, directory access and
Security persistence remain owned by Security.

All LDAP semantic properties remain under `sixpay.security.*`. Bootstrap is
the sole physical owner of runtime `application*.yml` files and composes the
LDAP runtime configuration under `backend/bootstrap/src/main/resources`.

LDAP service-account DN/password, trust material and other secrets are supplied
at runtime. Secret properties have no repository fallback/default value and
must never be logged, returned by APIs or committed.
