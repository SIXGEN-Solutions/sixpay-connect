# SIXPAY CONNECT — DA-6 Separate Authentication from Authorization

## 1. Scope

DA-6 makes SIXPAY the sole owner of business roles and permissions.

Authoritative revision policy:

```text
TASK_INVOCATION_OR_EXECUTION_ENVIRONMENT
```

`partner` remains the golden business-module reference. Authentication and
authorization implementation remain inside `backend/security`.

## 2. Existing implementation reviewed

Before DA-6:

- `AuthenticatedUser` already exposed `roles()` and `permissions()` through the
  canonical `SixpayPrincipal` contract.
- DA-5 already linked Local and OIDC identities to one canonical
  `security_user_accounts` user.
- Local authorization still came from `security_local_user_authorities`.
- OIDC authorization still flowed from JWT roles/scopes through
  `SixpayJwtAuthoritiesConverter` into `OidcAuthenticationAdapter`.
- The frontend still decoded JWT role claims through `extractSixpayRoles()`.

That meant identity was unified, but authorization ownership was still split by
authentication mechanism.

## 3. Target rule

```text
IdP proves identity.
SIXPAY owns business authorization.
```

Target flow:

```text
OIDC token
   │
   ├── issuer
   ├── subject
   └── identity attributes
         │
         ▼
ExternalIdentity
         │
         ▼
Linked SIXPAY User
         │
         ├── SIXPAY roles
         └── SIXPAY permissions
                 │
                 ▼
AuthenticatedUser / SixpayPrincipal
```

The same authorization data is loaded for Local authentication.

## 4. Canonical authorization persistence

DA-6 adds:

```text
security_user_roles
────────────────────────
user_id
role

security_user_permissions
────────────────────────
user_id
permission
```

Both tables reference `security_user_accounts`.

Roles are persisted without the Spring `ROLE_` prefix. The domain converts them
to Spring-compatible authorities only at the security boundary.

Permissions remain explicit strings such as:

```text
SCOPE_payment.read
payment.export
```

## 5. Migration from DA-3 Local authorization

Existing data in:

```text
security_local_user_authorities
```

is migrated to the canonical SIXPAY user:

```text
ROLE_ADMIN          -> security_user_roles.ADMIN
SCOPE_payment.read  -> security_user_permissions.SCOPE_payment.read
```

After successful migration, the credential-owned authorization table is dropped.

`security_local_users` continues to own only Local credential concerns:

```text
password_hash
credential status
failed attempts
temporary lock
last authenticated time
```

## 6. OIDC provider claims

The active OIDC path no longer calls `SixpayJwtAuthoritiesConverter`.

`OidcAuthenticationAdapter` reads only identity claims needed for linking:

```text
iss
sub
preferred_username / email
```

JWT claims such as:

```text
roles
groups
scope
realm_access
```

are not converted into SIXPAY business authorities.

`SixpayJwtAuthoritiesConverter` is retained only as a deprecated compatibility
class and now returns no business authorities.

A future explicit provider-group mapping feature may translate trusted provider
groups into controlled SIXPAY role-assignment operations, but provider groups
must never become runtime business authority implicitly.

## 7. Local authorization

Local authentication loads its linked canonical `SecurityUserAccountJpaEntity`.

The resulting principal authorities are derived from:

```text
security_user_roles
security_user_permissions
```

not from the Local credential row.

Therefore disabling/changing a role once on the SIXPAY user applies regardless
of whether the next login is Local or OIDC.

## 8. OIDC authorization

OIDC flow:

```text
valid JWT
   │
   ▼
OidcAuthenticationAdapter
   │
   ▼
ExternalIdentityResolver
   │
   ▼
security_user_identities
   │
   ▼
security_user_accounts
   │
   ├── security_user_roles
   └── security_user_permissions
   │
   ▼
AuthenticatedUser
```

The Spring `Authentication` authorities are rebuilt from the resolved
`AuthenticatedUser`, not from JWT claims.

## 9. Mechanism-neutral /me endpoint

`GET /api/v1/auth/me` becomes the canonical browser authorization source for
both Local and OIDC sessions.

It returns:

```json
{
  "subject": "canonical-sixpay-user-id",
  "username": "rodrigue",
  "roles": ["ADMIN"],
  "permissions": ["SCOPE_payment.read"]
}
```

The endpoint is moved to `AuthenticationSessionController` so it no longer
belongs conceptually to Local authentication.

The existing endpoint path remains unchanged.

## 10. Frontend authorization

The frontend no longer derives production roles from the OIDC access token.

After successful OIDC `checkAuth()`, it calls:

```text
GET /api/v1/auth/me
```

The bearer-token interceptor authenticates that request, and the frontend uses
the SIXPAY response as the authoritative role/permission snapshot.

`hasRole(...)` and `hasAnyRole(...)` remain conceptually unchanged.

DA-6 also exposes:

```text
hasPermission(...)
```

The old `extractSixpayRoles()` helper remains only for standalone/backward test
compatibility and is not used by the production OIDC session path.

## 11. Security invariant

A malicious or misconfigured IdP token containing:

```text
roles = [SUPER_ADMIN]
scope = everything
```

cannot grant those business authorities unless the linked SIXPAY user already
owns equivalent authorization inside SIXPAY.

## 12. No-change business boundary

No authentication-provider condition is introduced into:

```text
partner
payment
customer
accounting
reporting
incident
```

Existing `hasRole`, `hasAuthority`, `@PreAuthorize`, and policy usage continues
to operate against the canonical `AuthenticatedUser` authorities.

## 13. Tests

DA-6 adds/updates proof that:

- linked OIDC authorization comes from the SIXPAY user;
- provider JWT roles/scopes are ignored;
- Local and OIDC principals for the same user have identical roles and
  permissions;
- the legacy JWT converter no longer creates business authorities;
- the Resource Server still authenticates bearer tokens;
- frontend production OIDC session initialization obtains roles/permissions via
  `/api/v1/auth/me` rather than decoding JWT role claims.

## 14. Validation

Backend focused tests:

```bash
cd backend

mvn -pl security \
  -Dtest=LinkedExternalIdentityResolverTest,HybridAuthorizationConvergenceTest,HybridIdentityConvergenceTest,OidcAuthenticationAdapterTest,OidcAuthenticationProviderIT,SixpayJwtAuthoritiesConverterTest,SixpaySecurityAutoConfigurationTest,LocalAuthenticationServiceTest \
  test
```

Then:

```bash
mvn -pl security -am test
mvn -pl bootstrap -am test
```

Frontend:

```bash
cd ../frontend
npm test
npm run build
```

## 15. Exit criteria

```text
[ ] SIXPAY user owns roles
[ ] SIXPAY user owns permissions
[ ] Local credential storage owns no business authorization
[ ] OIDC JWT roles/scopes do not become business authorities
[ ] Local principal receives SIXPAY user authorization
[ ] OIDC principal receives SIXPAY user authorization
[ ] same user has same roles/permissions through Local and OIDC
[ ] /api/v1/auth/me is mechanism-neutral
[ ] frontend production OIDC roles come from SIXPAY /me
[ ] hasRole / hasAnyRole remain supported
[ ] provider-specific business dependency remains absent
[ ] focused tests pass
[ ] backend regression passes
[ ] frontend regression passes
```

Next lot:

```text
DA-7 — Hybrid Login UX
```
