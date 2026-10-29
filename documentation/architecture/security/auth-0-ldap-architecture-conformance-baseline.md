# SIXPAY CONNECT — AUTH-0
## LDAP Architecture & Conformance Baseline

### Status

```text
AUTH-0 — ARCHITECTURE BASELINE PREPARED
```

### Authoritative revision

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Branch: `feat/repository-baseline-consolidation-cucumber`
- Baseline SHA: `b219f71ae2d270a8eb37ef368408a20d52df5d9b`
- Scope: architecture/conformance baseline
- LDAP transport implementation: **NOT INCLUDED**
- LDAP infrastructure parameters: **NOT INVENTED**

## 1. Purpose

AUTH-0 extends the current human-authentication architecture from:

```text
LOCAL + OIDC
```

to the target capability model:

```text
LOCAL ──┐
OIDC ───┼──> canonical SIXPAY identity
LDAP ───┘       -> SixpayUserAccount
                -> SIXPAY-owned roles/permissions
                -> unified SIXPAY session
```

LDAP is a third authentication mechanism, not a second user model.

## 2. Mandatory reuse audit

AUTH-0 first audits the existing Local/OIDC foundations and freezes their reuse.

| Existing component | Current responsibility | LDAP conformance decision |
|---|---|---|
| `ExternalIdentity` | provider-neutral external identity `(issuer, subject, username)` | **REUSE**; LDAP must produce this model, not a LDAP-specific user model |
| `ExternalIdentityResolver` | canonical external identity -> `AuthenticatedUser` seam | **REUSE** |
| `LinkedExternalIdentityResolver` | linked identity -> canonical SIXPAY account + SIXPAY authorities | **REUSE**; generalized by identity type |
| `FindLinkedIdentityPort` | lookup by `(identityType, provider, providerSubject)` | **REUSE** |
| `UserIdentity` / `security_user_identities` | durable linked authentication identity | **REUSE** with `identity_type=LDAP` |
| `SixpayUserAccount` / `security_user_accounts` | canonical human SIXPAY account | **REUSE** unchanged |
| `security_user_roles` | SIXPAY-owned roles | **REUSE** unchanged |
| `security_user_permissions` | SIXPAY-owned permissions | **REUSE** unchanged |
| `AuthenticatedUser` / `SixpayPrincipal` | mechanism-neutral application principal | **REUSE** unchanged |
| `SpringSecuritySessionManager` | unified SIXPAY backend session | **REUSE** for future LDAP authentication |
| `AuthenticationSessionResponse` | canonical session response + method metadata | **REUSE**, with future `LDAP` method |
| frontend `/api/v1/auth/me` bootstrap | canonical session/authorization source | **REUSE** |
| frontend role/permission guards | authorization from SIXPAY session | **REUSE** unchanged |
| Partner machine identity surfaces | technical Partner caller identity | **DO NOT REUSE for human LDAP users** |

No second LDAP-specific account, principal, role store, permission store or
session model is authorized.

## 3. Canonical LDAP external identity

The existing `ExternalIdentity` triplet is retained:

```text
ExternalIdentity(
    issuer,
    subject,
    username
)
```

For LDAP its semantics are frozen as:

```text
issuer   = configured stable LDAP trust-domain identifier
subject  = stable immutable LDAP user identifier selected from bank-approved LDAP evidence
username = human-readable login/display identifier
```

The exact LDAP attribute used as `subject` is **TO_BE_CONFIRMED_WITH_LA_REGIONALE**.
AUTH-0 does not invent `uid`, `sAMAccountName`, `userPrincipalName`,
`objectGUID`, DN or any other directory attribute as the stable subject.

The `issuer` is not an OIDC issuer URI in LDAP mode. It is a stable configured
trust-domain key that distinguishes authoritative LDAP directories.

The canonical durable lookup remains:

```text
(identity_type, provider, provider_subject)

LDAP
+ trust-domain identifier
+ stable LDAP subject
    -> security_user_identities
    -> security_user_accounts
```

Email and mutable username remain forbidden as automatic linking keys.

## 4. Authentication vs authorization

LDAP proves human identity.

SIXPAY owns business authorization.

```text
LDAP bind / authentication
        |
        v
trusted LDAP external identity
        |
        v
linked SixpayUserAccount
        |
        +--> security_user_roles
        +--> security_user_permissions
        |
        v
AuthenticatedUser / SixpayPrincipal
```

### Explicit invariant

```text
LDAP group membership
    != SIXPAY role
    != SIXPAY permission
```

There is **no automatic LDAP group -> SIXPAY authority mapping** in AUTH-0.

A future controlled provisioning/synchronization capability may use directory
groups as administrative input only after a separate approved architecture and
security decision. Runtime LDAP group claims/memberships must never become
business authorities implicitly.

## 5. Identity linking policy

LDAP follows the conservative DA-5 linking model:

```text
LDAP identity must already be linked to a SixpayUserAccount
OR
it must be provisioned through an explicitly approved administrative process.
```

AUTH-0 authorizes no automatic linking by email, username, display name,
LDAP group or DN similarity.

## 6. Human LDAP vs Partner machine identity

```text
Human LDAP user
  -> ExternalIdentity
  -> security_user_identities
  -> SixpayUserAccount
  -> CurrentUserProvider / AuthenticatedUser

Partner technical caller
  -> CurrentMachineIdentityProvider
  -> security_partner_machine_identities
  -> Partner.partnerIdentifier
```

A Partner machine subject must never be represented as a LDAP human
`SixpayUserAccount`, and a LDAP user must never resolve through
`security_partner_machine_identities`.

## 7. Ownership

### Security

Security owns LDAP authentication adapters/ports in future AUTH lots,
normalization to `ExternalIdentity`, linked identity lookup, canonical
`SixpayUserAccount`, roles/permissions, session creation, authentication audit
and LDAP configuration validation.

### Bootstrap

Bootstrap owns composition only. It does not own LDAP business/authentication
rules.

### Frontend

Frontend owns presentation and session bootstrap. It consumes the canonical
backend SIXPAY session and `/api/v1/auth/me` authorization. It must not parse
LDAP groups into authorization or introduce a LDAP-specific authorization state.

### Business modules

Business modules remain authentication-mechanism neutral.

## 8. Session convergence

```text
LDAP authentication
    -> LDAP adapter
    -> ExternalIdentity
    -> linked canonical SixpayUserAccount
    -> AuthenticatedUser
    -> SpringSecuritySessionManager
    -> AuthenticationMethod.LDAP
    -> /api/v1/auth/me
```

LDAP password lifecycle is directory-owned unless a future approved bank
integration contract explicitly says otherwise. SIXPAY must not apply the
LOCAL password lifecycle to LDAP credentials.

## 9. Capability model

| LOCAL | OIDC | LDAP | Meaning |
|---:|---:|---:|---|
| 1 | 0 | 0 | Local only |
| 0 | 1 | 0 | OIDC only |
| 0 | 0 | 1 | LDAP only |
| 1 | 1 | 0 | Local + OIDC |
| 1 | 0 | 1 | Local + LDAP |
| 0 | 1 | 1 | OIDC + LDAP |
| 1 | 1 | 1 | Local + OIDC + LDAP |

AUTH-0 adds LDAP to the conceptual identity/method baseline only.
Runtime capability properties, LDAP connection settings, LDAP login endpoints
and frontend LDAP presentation belong to subsequent AUTH lots.

## 10. Conformance matrix

| Concern | LOCAL | OIDC | LDAP target | AUTH-0 decision |
|---|---|---|---|---|
| Canonical account | `SixpayUserAccount` | `SixpayUserAccount` | `SixpayUserAccount` | reuse |
| External identity model | local linked identity | `ExternalIdentity` | `ExternalIdentity` | reuse |
| Durable identity table | `security_user_identities` | `security_user_identities` | `security_user_identities` | extend type |
| Stable provider/trust domain | `SIXPAY` | exact issuer URI | configured LDAP trust-domain | define semantics |
| Stable provider subject | local subject | OIDC `sub` | bank-approved stable LDAP attribute | external decision required |
| Roles | SIXPAY | SIXPAY | SIXPAY | unchanged |
| Permissions | SIXPAY | SIXPAY | SIXPAY | unchanged |
| Provider groups as authorities | n/a | forbidden | forbidden | explicit |
| Principal | `AuthenticatedUser` | `AuthenticatedUser` | `AuthenticatedUser` | reuse |
| Backend session | unified | unified | unified | reuse |
| Session method | `LOCAL` | `OIDC` | `LDAP` | extend enum |
| Password lifecycle | SIXPAY | IdP | LDAP/directory | external owner |
| `/auth/me` | canonical | canonical | canonical | reuse |
| Frontend authorization | `/auth/me` | `/auth/me` | `/auth/me` | reuse |
| Partner M2M identity | separate | separate | separate | strict separation |

## 11. AUTH-0 code impact

AUTH-0 permits only:

```text
AuthenticationIdentityType += LDAP
AuthenticationMethod += LDAP
security_user_identities.identity_type allows LDAP
LinkedExternalIdentityResolver accepts an explicit identity type while
preserving the current OIDC entry point
```

AUTH-0 does **not** add Spring LDAP dependencies, LDAP URLs/secrets,
bind/search configuration, LDAP authentication endpoints, auto-provisioning,
group mapping, LDAP-specific account tables, LDAP-specific principal/session or
frontend LDAP UI.

## 12. External decisions required before runtime LDAP

La Régionale must provide/approve at least:

1. LDAP technology/profile;
2. endpoints per environment;
3. TLS/CA trust requirements;
4. bind strategy and secret ownership;
5. user search base and search/filter rules;
6. stable immutable LDAP attribute used as external subject;
7. accepted human login attribute(s);
8. disabled/locked/expired account semantics;
9. timeout/fail-closed expectations;
10. number of LDAP trust domains/directories;
11. password-expiry/change behavior;
12. audit/logging constraints for directory identifiers.

## 13. Validation commands

```bash
cd backend
mvn -pl security -Dtest=LinkedExternalIdentityResolverTest,AuthenticationCapabilitiesPropertiesTest,SixpaySecurityAutoConfigurationTest test
mvn -pl security -am test
mvn -pl bootstrap -am test
mvn verify
```

From repository root:

```bash
py scripts/verify_master_prompt_input_manifest.py
py scripts/verify_repository_hygiene.py
py scripts/verify_documentation_final.py
py scripts/verify_baseline.py
```

No validation result is claimed by AUTH-0 preparation.

## 14. Exit criteria

```text
[ ] existing identity/linking/session/capability components audited
[ ] LDAP represented as AuthenticationIdentityType
[ ] LDAP represented as AuthenticationMethod
[ ] no second LDAP identity/account model introduced
[ ] LDAP external identity triplet semantics frozen
[ ] stable LDAP subject remains explicit external decision
[ ] LDAP roles/permissions remain SIXPAY-owned
[ ] automatic LDAP group -> authority mapping explicitly forbidden
[ ] LDAP human identities separated from Partner machine identities
[ ] Security / Bootstrap / Frontend ownership documented
[ ] Local/OIDC/LDAP conformance matrix documented
[ ] no LDAP runtime dependency/configuration invented
[ ] focused tests pass
[ ] applicable repository gates pass
```

## 15. Next lot

```text
AUTH-1 — LDAP Environment & Trust Contract
```
