# SIXPAY CONNECT — DA-3 Local Authentication Consolidation

## 1. Scope

DA-3 consolidates the Local authentication path already used by the frontend.

Existing contract preserved:

```text
POST /api/v1/auth/login
GET  /api/v1/auth/me
POST /api/v1/auth/logout
```

The frontend continues to use `withCredentials: true`.

Authoritative branch:

```text
feat/repository-baseline-consolidation
```

The `partner` module remains the golden implementation and folder-structure reference.
Security remains owned by `backend/security`.

## 2. Existing implementation reviewed

The branch already contains:

```text
frontend/core/auth/LocalAuthenticationClient
AuthenticatedUser
SixpayPrincipal
CurrentUserProvider
SecurityContextCurrentUserProvider
AuthenticationCapabilitiesProperties
```

The backend filter chain was still `STATELESS` and OAuth2-resource-server oriented, so the
Local frontend contract did not yet have a complete persisted backend credential/session implementation.

## 3. Target structure

```text
security/src/main/java/com/sixpay/security/
├── api/
│   ├── controller/
│   │   └── LocalAuthenticationController
│   ├── dto/
│   └── error/
├── application/
│   ├── port/in/
│   │   ├── AuthenticateLocalUserUseCase
│   │   ├── GetCurrentSessionUseCase
│   │   └── LogoutUseCase
│   ├── port/out/
│   │   ├── LoadAuthenticationUserPort
│   │   ├── SaveAuthenticationUserStatePort
│   │   ├── PasswordVerificationPort
│   │   └── AuthenticationAuditPort
│   └── service/
├── domain/authentication/
└── infrastructure/authentication/
    ├── persistence/
    ├── password/
    ├── session/
    └── audit/
```

This follows the golden-module direction:

```text
api -> application -> domain
          ^
          |
   infrastructure adapters
```

## 4. Authentication flow

```text
username/password
      |
      v
LocalAuthenticationController
      |
      v
AuthenticateLocalUserUseCase
      |
      v
LocalAuthenticationService
      |
      +--> LoadAuthenticationUserPort
      +--> PasswordVerificationPort -> BCrypt
      +--> SaveAuthenticationUserStatePort
      +--> AuthenticationAuditPort
      |
      v
AuthenticatedUser implements SixpayPrincipal
      |
      v
SpringSecurityLocalSessionManager
      |
      v
HttpSession + secure cookie
```

No business module knows whether the principal came from Local or OIDC.

## 5. Password hashing

Passwords are never persisted in clear text.

DA-3 uses Spring Security BCrypt with configurable strength:

```yaml
sixpay.security.authentication.local.bcrypt-strength: 12
```

The default strength is 12.

## 6. Account status and brute-force protection

Supported Local account states:

```text
ACTIVE
DISABLED
```

Failed attempts are persisted. After the configured threshold, the account is temporarily locked.

Defaults:

```text
maximum failed attempts = 5
lock duration           = 15 minutes
```

Authentication lookup uses `PESSIMISTIC_WRITE` so concurrent failures for the same account cannot silently overwrite each other.

## 7. Credential errors and enumeration resistance

The public failure remains generic:

```text
401 Authentication failed / Invalid credentials
```

The response does not distinguish:

```text
unknown username
wrong password
disabled account
temporarily locked account
```

A dummy BCrypt verification is executed for unknown/disabled/locked identities to reduce trivial timing-based username enumeration.

## 8. Session lifecycle

Local authentication uses a server-side Spring Security session because the frontend contract already uses cookies through `withCredentials: true`.

The filter chain uses:

```text
SessionCreationPolicy.IF_REQUIRED
```

so Local may create an HttpSession while Bearer-token requests remain compatible.

Session fixation protection is explicit: any pre-authentication session is invalidated before the authenticated SecurityContext is persisted.

Session timeout is externalized:

```yaml
server.servlet.session.timeout: ${SIXPAY_LOCAL_SESSION_TIMEOUT:30m}
```

## 9. Cookie security and CSRF

Provided Local/Hybrid profiles configure the session cookie with:

```text
HttpOnly = true
SameSite = Strict
Secure   = true by default
```

`SIXPAY_SESSION_COOKIE_SECURE=false` is intended only for controlled local HTTP development.

Because Local authentication uses cookies, CSRF is enabled for cookie-authenticated mutations.

Rules:

```text
Bearer request               -> CSRF ignored
POST /api/v1/auth/login      -> CSRF ignored before authentication
cookie-authenticated mutation -> CSRF required
```

An `XSRF-TOKEN` cookie is issued after Local login and cleared on logout, matching Angular's same-origin XSRF support.

## 10. Current session and logout use cases

`GET /api/v1/auth/me` delegates to `GetCurrentSessionUseCase`.

`POST /api/v1/auth/logout` delegates to `LogoutUseCase`, records an audit event, then terminates the Spring Security session and clears the CSRF token.

This keeps HTTP/session mechanics outside the application service layer.

## 11. Audit

DA-3 persists append-only Local authentication audit records in:

```text
security_authentication_audit
```

Events currently covered:

```text
LOGIN SUCCESS
LOGIN FAILURE
LOGOUT SUCCESS
```

No password, password hash, token, authorization code, client secret or session identifier is written to the audit table.

The table is protected by a PostgreSQL trigger that rejects UPDATE and DELETE, matching the append-only discipline used by the golden `partner` module.

## 12. Database ownership

Runtime Flyway migrations remain centralized in `backend/bootstrap`, consistent with the repository's golden pattern.

DA-3 adds:

```text
V202608102130__security_local_authentication.sql
```

which creates:

```text
security_local_users
security_local_user_authorities
security_authentication_audit
```

No default production account/password is seeded.

## 13. No-change boundary

No Local/OIDC branching is introduced in:

```text
partner
payment
customer
accounting
reporting
incident
```

They continue consuming the canonical SIXPAY identity and authorization model.

## 14. Explicit non-scope

DA-3 does not implement:

```text
forgot password
reset password
change password
password administration UI
OIDC browser login
external identity linking
final hybrid session arbitration
```

Those remain later dedicated lots.

## 15. Validation

From `backend/`:

```bash
mvn -pl security \
  -Dtest=LocalAuthenticationUserTest,LocalAuthenticationServiceTest,LocalLogoutServiceTest,AuthenticatedUserTest,SecurityContextCurrentUserProviderTest,AuthenticationCapabilitiesPropertiesTest,SixpaySecurityAutoConfigurationTest \
  test

mvn -pl security -am test
mvn -pl bootstrap -am test
```

Frontend regression:

```bash
cd ../frontend
npm test
npm run build
```

## 16. Exit criteria

```text
[ ] existing /login /me /logout contract preserved
[ ] withCredentials frontend behavior unchanged
[ ] BCrypt password verification implemented
[ ] clear-text password persistence impossible by schema/design
[ ] ACTIVE/DISABLED account status enforced
[ ] failed attempts persisted
[ ] temporary locking enforced
[ ] session timeout configurable
[ ] session fixation protection implemented
[ ] logout terminates server-side session
[ ] generic credential errors returned
[ ] Local audit persisted append-only
[ ] Local success produces canonical AuthenticatedUser/SixpayPrincipal
[ ] business modules remain authentication-mechanism agnostic
[ ] focused security tests pass
[ ] backend regression tests pass
[ ] frontend regression tests pass
```

Next lot:

```text
DA-4 — OIDC Authentication
```
