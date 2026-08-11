# SIXPAY CONNECT — DA-9 Administration, operational security and audit

## Scope

DA-9 materializes the security-administration capability while preserving the
ownership established by DA-0..DA-8:

```text
administration module -> administrative HTTP boundary
security module       -> users, identities, credentials, authorization, audit
partner/payment/...   -> no authentication-mechanism knowledge
```

The authoritative branch is `feat/hybrid-authentification-system`; `partner`
remains the structural golden reference.

## Existing baseline reviewed

Before DA-9, `backend/administration` was intentionally only a Maven shell. The
frontend Administration feature was a Phase-7 mock. Security already owned:

- canonical `security_user_accounts`;
- Local credentials;
- linked OIDC identities;
- SIXPAY roles and permissions;
- unified backend session.

DA-9 therefore does **not** move security persistence into Administration.
Administration exposes controlled use cases over the security-owned boundary.

## User administration view

```text
Identity
  Account
    Username
    Email
    Status

  Authentication methods
    Local enabled/disabled
    OIDC linked/not linked

  External identities
    Type
    Provider (OIDC issuer)
    Subject
    Status LINKED

  Authorization
    SIXPAY roles
    SIXPAY permissions

  Operational security
    Last authentication
    Recent security events
```

## Supported actions

```text
Enable/disable existing Local login
Reset Local password
Link OIDC identity
Unlink OIDC identity
Disable canonical SIXPAY user
Review recent authentication/security events
```

All mutation endpoints require `ROLE_ADMIN`.

## API

```text
GET    /internal/api/v1/administration/users
GET    /internal/api/v1/administration/users/{userId}
PUT    /internal/api/v1/administration/users/{userId}/authentication-methods/local
POST   /internal/api/v1/administration/users/{userId}/local-password-reset
POST   /internal/api/v1/administration/users/{userId}/identities/oidc
DELETE /internal/api/v1/administration/users/{userId}/identities/{identityId}
POST   /internal/api/v1/administration/users/{userId}/disable
```

## Conservative rules

- OIDC linking is explicit and admin-controlled.
- No email-based automatic linking is introduced.
- Provider remains provider-neutral and stores the trusted OIDC issuer.
- A Local password reset hashes the submitted password immediately with BCrypt.
- Disabling a canonical user blocks both Local and OIDC through the existing
  canonical-user status checks.
- Unlink only accepts an OIDC identity owned by the requested user.
- Local enablement requires an already provisioned Local credential record;
  creation of a new Local credential for an OIDC-only user is deliberately not
  hidden inside a boolean toggle.

## Operational audit

DA-9 adds append-only `security_audit_events` with these event types:

```text
LOGIN_SUCCESS
LOGIN_FAILURE
LOGOUT
PASSWORD_RESET
ACCOUNT_LOCKED
OIDC_LOGIN_SUCCESS
OIDC_LOGIN_FAILURE
IDENTITY_LINKED
IDENTITY_UNLINKED
AUTH_METHOD_ENABLED
AUTH_METHOD_DISABLED
USER_DISABLED
```

Local login/logout events are bridged from the existing Local audit port.
Account locking is emitted when a failed login crosses the lock threshold.
OIDC success is emitted after canonical identity resolution. Bearer/OIDC
failures are recorded by the authentication entry point without persisting the
bearer token.

## Never audited/logged

The DA-9 audit model has no fields for and must never receive:

```text
password
password hash
access token
refresh token
authorization code
client secret
session cookie
```

`PASSWORD_RESET` records only the action, actor, target and time. The password
request body is hashed before persistence and is never copied to audit detail.

## Database immutability

`security_audit_events` is protected with a PostgreSQL trigger rejecting UPDATE
and DELETE. Runtime migrations remain centralized in `backend/bootstrap`.

## Frontend

Administration gains:

```text
/administration/users
/administration/users/:userId
```

The detail view exposes account status, Local/SSO state, linked external
identities, roles/permissions and recent security events. The existing ADMIN
route guard remains in place.

## No-change boundary

No DA-9 dependency is introduced into:

```text
partner
payment
customer
accounting
reporting
incident
```

## Validation

```bash
cd backend
mvn -pl security,administration -am -DskipTests compile
mvn -pl security,administration -am test
mvn -pl bootstrap -am test

cd ../frontend
npm test
npm run build
```

## Exit criteria

```text
[ ] user identity/account administration is visible to ADMIN
[ ] Local method can be enabled/disabled when provisioned
[ ] Local password reset hashes before persistence
[ ] OIDC identity can be explicitly linked/unlinked
[ ] canonical user can be disabled
[ ] roles/permissions remain SIXPAY-owned and read-only in DA-9 UI
[ ] recent authentication/security events can be reviewed
[ ] required security events are append-only audited
[ ] no password/token/code/client-secret field exists in audit storage
[ ] business modules remain authentication-provider agnostic
```
