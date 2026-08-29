# SIXPAY CONNECT — DA-7 Hybrid Login UX

## Scope

DA-7 turns the existing mutually-exclusive login presentation into a capability-driven hybrid login.

Authoritative revision policy:

```text
TASK_INVOCATION_OR_EXECUTION_ENVIRONMENT
```

## Existing implementation reviewed

The current login component uses:

```text
if Local -> local form
else if OIDC -> SSO button
```

DA-2 already made Local and OIDC independent capabilities, and production currently enables both.

The current `authenticationGuard` checks only `isAuthenticated()` after `ready$`; this is already mechanism-neutral and requires no DA-7 change.

## Target presentation

```text
@if (localEnabled) {
  Local form
}

@if (localEnabled && oidcEnabled) {
  OR divider
}

@if (oidcEnabled) {
  SSO button
}
```

Possible configurations:

| Local | OIDC | Login UI |
|---|---|---|
| true | false | Local form only |
| false | true | SSO only |
| true | true | Local form + OR + SSO |

Standalone remains development/demo-only.

## Active authentication method

DA-7 introduces runtime state:

```text
activeAuthenticationMethod = 'local' | 'oidc' | null
```

This is not a configuration mode. It records which mechanism established the current runtime session.

It is used for:

```text
logout
bearer-token attachment
session expiry handling
hybrid bootstrap
```

## Hybrid bootstrap

When both capabilities are enabled:

1. OIDC client checks whether an OIDC session/callback exists;
2. if OIDC is authenticated, active method becomes `oidc` and `/api/v1/auth/me` loads the canonical SIXPAY session;
3. otherwise the client attempts the existing Local cookie session through `/api/v1/auth/me`;
4. if neither exists, the application is ready but unauthenticated and the guard sends the user to `/login`.

This removes the DA-2 temporary OIDC-only bootstrap precedence.

## Request authentication

The HTTP interceptor no longer asks whether OIDC is merely enabled.

It asks the `AuthenticationService` for the token associated with the active session.

Therefore:

```text
active method = local -> no Authorization bearer header
active method = oidc  -> OIDC access token attached
active method = null  -> no bearer token
```

This prevents a stale OIDC token from being attached while the user is actively using a Local session.

## Logout

Logout is now based on the active authentication method:

```text
LOCAL -> POST /api/v1/auth/logout + clear Local session
OIDC  -> OIDC revoke/logoff
null  -> clear local frontend state
```

OIDC being enabled no longer forces an OIDC logout for a Local-authenticated user.

## Forgot password

DA-7 does not invent a password-recovery workflow. The login card displays a conservative hint to contact a SIXPAY administrator. A real reset route belongs to a dedicated password-management lot.

## No-change boundary

No change is required in:

```text
authentication.guard.ts
backend security
partner
payment
customer
accounting
reporting
```

## Validation

```bash
cd frontend
npm test
npm run build
```

## Exit criteria

```text
[ ] Local-only UI still works
[ ] OIDC-only UI still works
[ ] Hybrid UI displays Local + OR + SSO simultaneously
[ ] localEnabled and oidcEnabled replace mode-based login presentation
[ ] activeAuthenticationMethod tracks Local/OIDC runtime session
[ ] Local logout does not invoke OIDC logout
[ ] OIDC logout does not invoke Local logout
[ ] bearer token is attached only for an active OIDC session
[ ] hybrid bootstrap can recover either OIDC or Local session
[ ] authenticationGuard remains mechanism-neutral
[ ] frontend tests pass
[ ] frontend build passes
```
