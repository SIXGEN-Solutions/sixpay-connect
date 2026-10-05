# SIXPAY CONNECT — AUTH-5
## Unified Session & Frontend Authentication Capabilities

### Status

```text
AUTH-5 — IMPLEMENTED; FINAL VALIDATION REQUIRED
```

### Current baseline

- Capability owner: Security
- Frontend session source of truth: `GET /api/v1/auth/me`
- Runtime methods: `LOCAL`, `OIDC`, `LDAP`
- LDAP runtime login: `POST /api/v1/auth/login/ldap`

## Unified session source of truth

`GET /api/v1/auth/me` is the canonical frontend session source for LOCAL,
OIDC and LDAP.

The canonical response exposes the active authentication method plus backend
runtime capabilities:

```text
localEnabled
oidcEnabled
ldapEnabled
```

After a backend session exists, these backend capabilities are authoritative.
Before session establishment, the Angular environment is only the bootstrap
fallback used to present available login mechanisms.

## Frontend authentication flows

```text
LOCAL credentials
    -> POST /api/v1/auth/login
    -> canonical SIXPAY session

OIDC bearer
    -> POST /api/v1/auth/session/oidc
    -> canonical SIXPAY session

LDAP credentials
    -> POST /api/v1/auth/login/ldap
    -> canonical SIXPAY session

all providers
    -> GET /api/v1/auth/me
    -> SIXPAY roles / permissions / active method
```

The login component presents only enabled providers. LDAP uses its dedicated
directory credential form. LDAP credentials are transient and are not retained
as frontend authorization state.

Logout remains provider-neutral at the backend through
`POST /api/v1/auth/logout`. OIDC may additionally revoke/log off its provider
tokens client-side. LOCAL and LDAP have no provider token lifecycle in the SPA.

## Authorization independence

Roles and permissions come exclusively from the canonical SIXPAY session.
Frontend guards remain authentication-provider neutral. LDAP groups and OIDC
provider roles/scopes never become SIXPAY authorities implicitly.

The SIXPAY LOCAL password-change lifecycle applies only when the active method
is LOCAL. OIDC and LDAP password lifecycle remains provider-owned.

## Validation

Backend:

```bash
cd backend
mvn -pl security -Dtest=AuthenticationSessionResponseTest,AuthenticationCapabilitiesPropertiesTest,AuthenticationProviderPolicyValidatorTest test
mvn -pl security,bootstrap -am test
mvn verify
```

Frontend:

```bash
cd frontend
npm run test -- --run
npm run verify:quality
npm run verify:ci
```

Repository root:

```bash
py scripts/verify_master_prompt_input_manifest.py
py scripts/verify_repository_hygiene.py
py scripts/verify_documentation_final.py
py scripts/verify_baseline.py
```

No validation result is claimed until these commands complete with exit code 0.
