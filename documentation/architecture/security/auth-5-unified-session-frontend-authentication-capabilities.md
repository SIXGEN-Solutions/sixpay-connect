# SIXPAY CONNECT — AUTH-5
## Unified Session & Frontend Authentication Capabilities

### Status

```text
AUTH-5 — IMPLEMENTATION PREPARED; LDAP LOGIN UX DECISION DEFERRED
```

### Baseline

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Baseline SHA: `26d1cc1f9dd41d62b9164b584596d92353cb00fa`
- Prerequisite: AUTH-4 capability/runtime configuration
- New LDAP login endpoint: **NO**
- LDAP credential UX: **NOT DECIDED BY THIS LOT**

## Session source of truth

`GET /api/v1/auth/me` remains the canonical frontend session source.

The canonical session model supports:

```text
LOCAL
OIDC
LDAP
```

The session response also exposes backend runtime capabilities:

```text
localEnabled
oidcEnabled
ldapEnabled
```

Capabilities indicate available authentication mechanisms only. They never
grant SIXPAY roles or permissions.

## Authorization independence

Roles and permissions continue to come exclusively from the canonical SIXPAY
user/session. Frontend guards remain independent from the authentication
provider.

LDAP groups are never interpreted as SIXPAY authorities.

## Bootstrap flow

```text
existing SIXPAY session?
    yes -> use /api/v1/auth/me
    no  -> expose enabled providers
```

The existing OIDC bootstrap/exchange remains unchanged.

LOCAL keeps its existing SIXPAY login path.

LDAP is represented as an available capability and as a possible active session
method, but AUTH-5 does not invent how LDAP credentials are collected or
submitted.

## Deferred decision

The current sources do not establish whether LDAP authentication uses:

- a SIXPAY username/password form backed by an LDAP-specific endpoint; or
- another bank-approved interaction mechanism.

Therefore AUTH-5 deliberately creates no LDAP login endpoint and no executable
LDAP credential form.

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
