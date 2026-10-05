# SIXPAY CONNECT — LDAP Administration / Provisioning Operational Runbook

## Status

Canonical current-state operational documentation for LDAP/Active Directory
administrative provisioning.

The authoritative implementation revision is selected at task/runtime level;
this document does not pin a feature branch or temporary SHA.

The registered administration contract is
`security-user-administration-api-v1` with:

```text
lifecycleStatus: ACTIVE_MVP
approvalStatus: APPROVED
generationPolicy: ACTIVE
codeGenerationAllowed: true
```

The contract registry remains authoritative for these lifecycle and generation
attributes.

## Ownership

Security owns:

- LDAP/Active Directory authentication semantics;
- exact directory-user discovery;
- directory account-state normalization;
- stable external identity mapping;
- canonical SIXPAY users and LDAP identity links;
- SIXPAY roles and permissions;
- provisioning transaction and Security audit.

Administration owns the ADMIN-only HTTP and frontend delivery boundary. It
uses reviewed Security public application surfaces and does not access Security
infrastructure, LDAP/JNDI implementation classes, JPA entities or repositories.

Bootstrap is the sole physical owner of runtime `application*.yml`
configuration. Security remains the semantic owner of `sixpay.security.*`.

## Canonical administrative provisioning flow

```text
ADMIN authenticated in SIXPAY
        |
Administration > Utilisateurs
        |
choose LDAP / Active Directory
        |
exact lookup by configured directory login identifier
        |
read-only directory projection
(username, displayName, email, accountStatus)
        |
administrator selects SIXPAY roles / permissions
        |
POST provisioning
        |
Security re-resolves directory identity
        |
require directory accountStatus = ACTIVE
        |
check immutable LDAP identity conflict
        |
check canonical SIXPAY username conflict
        |
create canonical SIXPAY account
LOCAL authentication disabled
        |
link (LDAP, trust-domain, objectGUID)
        |
persist SIXPAY roles / permissions
        |
emit Security audit
```

The frontend does not submit LDAP credentials, trust-domain or objectGUID in
the provisioning request. The server obtains and validates directory identity
data itself.

An existing LDAP identity is `ALREADY_PROVISIONED`. An existing SIXPAY
username for a different/unlinked directory identity is `USERNAME_CONFLICT`.
Provisioning never auto-links an existing canonical account.

## Directory discovery

The approved discovery mode is an exact lookup by the configured LDAP login
identifier.

The Administration projection contains:

- username;
- display name when available;
- email when available;
- normalized directory account status;
- immutable stable subject as read-only API transport data.

For the approved Active Directory profile, the stable subject is `objectGUID`.
The Administration UI does not expose it as an editable value.

Fuzzy search, bulk synchronization and LDAP-group-driven provisioning are not
part of this contract version.

## Directory account states

Normalized states are:

```text
ACTIVE
DISABLED
LOCKED
EXPIRED
PASSWORD_EXPIRED
PASSWORD_CHANGE_REQUIRED
```

Only `ACTIVE` is provisionable.

Disabled, locked, expired or password-constrained directory identities fail
closed. A directory outage must never be interpreted as user deletion or as
authorization to bypass LDAP.

## Identity and authorization invariant

LDAP/Active Directory proves directory identity. SIXPAY owns application
authorization.

The durable Active Directory link is:

```text
identityType = LDAP
provider     = configured trust-domain
subject      = immutable objectGUID
```

DN, username, email, display name and LDAP groups are not durable identity-link
keys.

**LDAP groups are never converted implicitly into SIXPAY roles or
permissions.** Roles and permissions are selected and persisted through
SIXPAY-owned administration.

## Login after provisioning

After provisioning, LDAP login follows the Security authentication runtime:

```text
LDAP credentials
    -> directory authentication
    -> stable LDAP identity
    -> linked canonical SIXPAY account
    -> account ACTIVE check
    -> SIXPAY roles / permissions
    -> canonical SIXPAY session
```

LDAP credentials remain transient and directory-owned. SIXPAY does not persist
the LDAP user's password.

`GET /api/v1/auth/me` remains the frontend session source of truth after
authentication.

## HTTP administration boundary

Read-only discovery:

```text
GET /internal/api/v1/administration/directory-users/{username}
```

Provisioning:

```text
POST /internal/api/v1/administration/directory-users/{username}/provisioning
```

The provisioning body contains only SIXPAY-owned authorization assignments:

```json
{
  "roles": ["<approved SIXPAY role>"],
  "permissions": ["<approved SIXPAY permission>"]
}
```

The request does not accept an LDAP password, service-account credential,
trust-domain or objectGUID.

Existing generic identity administration remains available through the
registered user-administration contract; it does not change the canonical
directory-provisioning flow above.

## Runtime configuration

LDAP semantic configuration remains under:

```text
sixpay.security.authentication.ldap.*
```

Physical runtime YAML remains under Bootstrap, including:

```text
backend/bootstrap/src/main/resources/config/security/ldap-common.yml
```

The LDAP service-account credentials are runtime injected:

```text
SIXPAY_LDAP_SERVICE_ACCOUNT_DN
SIXPAY_LDAP_SERVICE_ACCOUNT_PASSWORD
```

These secret properties must not have repository default/fallback values.
Secrets must not be committed, returned by APIs, written to audit detail or
logged.

Non-secret configuration may have reviewed defaults where already defined by
the runtime baseline. No separate YAML profile matrix is introduced for LDAP
provider combinations.

## LDAP outage runbook

When directory discovery or authentication is unavailable:

1. confirm the LDAP capability is enabled for the runtime;
2. inspect dependency health and technical metrics without identity data;
3. verify DNS/network/TLS reachability to configured LDAPS endpoints;
4. distinguish timeout, TLS/certificate and provider-unavailable failures;
5. do not bypass LDAP authentication;
6. do not auto-link another identity;
7. do not derive roles from LDAP groups;
8. restore the directory/network/trust dependency;
9. re-test with a controlled non-production account;
10. record the operational incident through the existing incident process.

Other independently enabled authentication providers may continue according to
their own configuration; LDAP failure does not silently change a user's
authentication method.

## Observability and sensitive data

Allowed operational signals include aggregate provider availability, bounded
timeout/TLS/failover metrics and authentication success/failure counters with
non-sensitive labels.

Do not expose in logs, metrics, audit detail, health responses or frontend
messages:

- LDAP passwords or service-account secrets;
- bind configuration secrets;
- user DN;
- objectGUID/stable subject;
- LDAP search-result payloads;
- session or token material.

## Persistence and concurrency

Security owns the canonical persistence. The existing schema protects the
identity and username invariants with unique constraints, including the stable
external identity tuple and normalized canonical username.

A concurrent provisioning attempt must therefore result in one canonical
account/link and a deterministic business conflict for the competing request;
it must never create two canonical users for the same LDAP identity.

No schema evolution is introduced by this documentation lot.

## Validation

Run targeted validations before global gates.

Backend Security:

```bash
cd backend
mvn -pl security -am -Dsurefire.failIfNoSpecifiedTests=false test
```

Administration:

```bash
cd backend
mvn -pl administration -am -Dsurefire.failIfNoSpecifiedTests=false test
```

Frontend Administration:

```bash
cd frontend
npx ng test --watch=false --include="src/app/features/administration/components/security-user-create-page.component.spec.ts"
npx ng test --watch=false --include="src/app/features/administration/services/security-user-administration.service.spec.ts"
npm run lint
```

Configuration/documentation gates:

```bash
py scripts/verify_configuration_consolidation.py
py scripts/verify_spring_configuration_hygiene.py
py scripts/verify_documentation_contract_references.py
py scripts/verify_documentation_final.py
```

Global gates:

```bash
cd backend
mvn verify
```

```bash
cd frontend
npm run verify:quality
```

Repository-level validation:

```bash
py scripts/verify_master_prompt_input_manifest.py
py scripts/verify_master_engineering_prompt.py
py scripts/verify_repository_hygiene.py
py scripts/verify_baseline.py
git diff --check
git status --short
```

A validation is reported as passed only after its command exits successfully.
Docker-dependent full-test gates remain subject to the local environment.
