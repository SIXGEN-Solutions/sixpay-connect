# SIXPAY CONNECT — DA-8 Unified Frontend Session Bootstrap

## Scope

DA-8 removes the remaining dual-session bootstrap behavior from the frontend.

Authoritative branch:

```text
feat/repository-baseline-consolidation-cleanup
```

`partner` remains the golden business-module reference. Authentication/session
ownership remains in `backend/security` and `frontend/core/auth`.

## Existing implementation reviewed

Before DA-8, DA-7 still used two runtime session models:

```text
Local -> backend HttpSession cookie
OIDC  -> frontend OIDC token + bearer on API requests
```

The frontend first inspected OIDC and only then fell back to a Local backend
session. `activeAuthenticationMethod` was therefore also needed by the
interceptor to decide whether to attach an access token.

The backend already had the foundations required to converge both paths:

```text
AuthenticatedUser / SixpayPrincipal
AuthenticationSessionController
SecurityContextRepository
SessionCreationPolicy.IF_REQUIRED
```

## Target architecture

DA-8 implements the preferred architecture:

```text
LOCAL login ───────────────┐
                           │
                           ▼
                    SIXPAY backend session
                           ▲
                           │
OIDC bearer after callback ┘
```

The OIDC token is used once to establish a SIXPAY backend session.
Normal business requests then use the same secure backend session cookie as
Local authentication.

## Bootstrap priority

The frontend bootstrap is now:

```text
initializeAuthentication()
        │
        ├── GET /api/v1/auth/me
        │      │
        │      ├── 200 -> existing SIXPAY backend session wins
        │      │
        │      └── 401 -> continue
        │
        ├── OIDC checkAuth()
        │      │
        │      ├── authenticated
        │      │      │
        │      │      ▼
        │      │  POST /api/v1/auth/session/oidc
        │      │  Authorization: Bearer <access token>
        │      │      │
        │      │      ▼
        │      │  SIXPAY backend session
        │      │
        │      └── not authenticated -> anonymous
        │
        ▼
resolveAuthenticatedState()
```

This is stronger than a frontend-only `Local first / OIDC second` priority:
once a backend SIXPAY session exists, there is only one application session to
recover.

## OIDC session exchange

New endpoint:

```text
POST /api/v1/auth/session/oidc
```

Requirements:

- request must already be authenticated through the OIDC Resource Server;
- `OidcAuthenticationAdapter` has already linked the identity and loaded SIXPAY
  authorization;
- the controller refuses a non-OIDC Spring Authentication;
- the canonical `AuthenticatedUser` is persisted into the same Spring Security
  session used by Local login;
- a CSRF token is issued for subsequent cookie-authenticated mutations.

The access token is not stored in the backend session.

## Authentication method metadata

The backend session stores only the method that established the SIXPAY session:

```text
LOCAL
OIDC
```

`GET /api/v1/auth/me` returns that metadata with the canonical session:

```json
{
  "subject": "canonical-user-id",
  "username": "rodrigue",
  "roles": ["ADMIN"],
  "permissions": ["SCOPE_payment.read"],
  "authenticationMethod": "OIDC"
}
```

The frontend therefore no longer guesses the active method during bootstrap.

## Request authentication after bootstrap

Normal SIXPAY API requests no longer receive an OIDC bearer token from the
frontend interceptor.

The interceptor now only guarantees `withCredentials=true` for SIXPAY API
requests.

Bearer usage is limited to the explicit OIDC-to-backend-session exchange.

This removes the possibility of a stale OIDC token competing with an existing
Local/SIXPAY backend session.

## Logout

Logout becomes backend-session-first for both mechanisms:

```text
POST /api/v1/auth/logout
        │
        ▼
invalidate SIXPAY backend session
        │
        ├── session method LOCAL -> navigate to login
        │
        └── session method OIDC  -> revoke/logoff IdP browser session
```

The backend logout endpoint is therefore mechanism-neutral and is moved to
`AuthenticationSessionController`.

## Local controller responsibility

After DA-8 `LocalAuthenticationController` owns only:

```text
POST /api/v1/auth/login
```

`/me` and `/logout` are owned by the mechanism-neutral session controller.

## Session manager responsibility

`SpringSecuritySessionManager` replaces Local-specific session ownership.

The previous `SpringSecurityLocalSessionManager` class remains only as a
deprecated source-compatibility wrapper and is no longer the active bean.

## Security properties

DA-8 preserves:

```text
server-side HttpSession
session fixation protection
HttpOnly session cookie
SameSite policy
CSRF token after session creation
401 for unauthenticated protected resources
SIXPAY-owned authorization
OIDC issuer/sub identity linking
```

## No-change boundary

No authentication-mode/session decision is introduced in:

```text
partner
payment
customer
accounting
reporting
incident
```

The existing `authenticationGuard` remains mechanism-neutral and does not need
modification.

## Validation

Backend:

```bash
cd backend
mvn -pl security -DskipTests compile
mvn -pl security -am test
mvn -pl bootstrap -am test
```

Frontend:

```bash
cd ../frontend
npm test
npm run build
```

## Exit criteria

```text
[ ] existing backend SIXPAY session is checked first
[ ] Local login creates unified backend session
[ ] OIDC callback/token is exchanged for unified backend session
[ ] normal API calls no longer depend on frontend bearer token attachment
[ ] /me returns canonical identity + roles + permissions + auth method
[ ] /logout invalidates the same backend session for Local and OIDC
[ ] OIDC logout also clears/revokes provider-side browser state
[ ] no two competing SIXPAY application sessions are selected by frontend logic
[ ] authenticationGuard remains mechanism-neutral
[ ] business modules remain unchanged
[ ] backend tests pass
[ ] frontend tests/build pass
```
