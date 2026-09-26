# SIXPAY CONNECT — AUTH-1
## LDAP Trust & Environment Contract

### Status

```text
AUTH-1 — CONTRACT PREPARED; HUMAN APPROVAL BY LA REGIONALE REQUIRED
```

### Authoritative revision

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Branch: `feat/repository-baseline-consolidation-cucumber`
- Baseline SHA: `cf7a0d8b1c0ee3a9ccb0133084246daab58d57fe`
- Prerequisite: `AUTH-0 — LDAP Architecture & Conformance Baseline`
- Runtime LDAP implementation: **NOT INCLUDED**
- Human gate: **MANDATORY — LA REGIONALE**

## 1. Purpose

AUTH-1 freezes the technical and trust contract required before SIXPAY may
implement LDAP authentication against La Régionale's directory.

AUTH-1 is broader than a simple host/port/Base-DN inventory. It covers:

```text
network reachability
+ TLS trust
+ bind/authentication strategy
+ directory discovery/search semantics
+ canonical LDAP external identity semantics
+ failover/timeouts
+ secret ownership/rotation
+ account-state semantics
+ operational/audit constraints
```

No LDAP runtime adapter may be considered production-ready before this contract
is completed and explicitly approved by La Régionale.

## 2. AUTH-0 invariants preserved

AUTH-1 does not change the AUTH-0 identity architecture:

```text
LOCAL ──┐
OIDC ───┼──> canonical SIXPAY identity
LDAP ───┘       -> SixpayUserAccount
                -> SIXPAY roles/permissions
                -> unified SIXPAY session
```

The following remain mandatory:

- LDAP uses the existing `ExternalIdentity` model;
- LDAP uses `security_user_identities` and `SixpayUserAccount`;
- SIXPAY owns roles and permissions;
- no automatic LDAP group -> SIXPAY authority mapping;
- LDAP human users are separate from Partner machine identities;
- email/username are not automatic linking keys;
- no second LDAP-specific user/account/session model is introduced.

## 3. Contract completion rule

Every field below must be classified as one of:

```text
APPROVED
NOT_APPLICABLE
TO_BE_CONFIRMED
```

AUTH-1 is not approved while any **mandatory** field remains
`TO_BE_CONFIRMED`.

No secret value is stored in this document or anywhere in Git.

## 4. Directory endpoint contract

### 4.1 LDAP/LDAPS endpoints

| Item | Required | Bank value/status |
|---|---:|---|
| Primary directory URL | YES | `TO_BE_CONFIRMED` |
| Secondary/failover directory URL(s) | NO/IF_AVAILABLE | `TO_BE_CONFIRMED` |
| Protocol | YES | `TO_BE_CONFIRMED` (`ldap://` / `ldaps://`) |
| Port(s) | YES | `TO_BE_CONFIRMED` |
| Environment scope | YES | `TO_BE_CONFIRMED` |
| DNS ownership/resolution requirements | YES | `TO_BE_CONFIRMED` |

The bank must identify endpoints by environment where applicable
(sandbox/integration/preproduction/production).

Raw IP addresses should not be used as the long-term trust identity unless the
bank explicitly requires them.

## 5. Transport security and trust

### 5.1 TLS mode

La Régionale must approve exactly one applicable transport posture:

```text
LDAPS from connection establishment
OR
LDAP + StartTLS
```

Unencrypted LDAP authentication is **not approved by AUTH-1**.

| Item | Required | Bank value/status |
|---|---:|---|
| TLS/LDAPS mandatory | YES | `TO_BE_CONFIRMED` |
| LDAPS or StartTLS | YES | `TO_BE_CONFIRMED` |
| Minimum TLS version | YES | `TO_BE_CONFIRMED` |
| Server certificate hostname/SAN expectation | YES | `TO_BE_CONFIRMED` |
| Private/public CA model | YES | `TO_BE_CONFIRMED` |
| CA chain delivery mechanism | YES | `TO_BE_CONFIRMED` |
| Certificate expiry/renewal ownership | YES | `TO_BE_CONFIRMED` |

### 5.2 Trust material

The bank must provide the certificate authority chain or approved trust
mechanism required by SIXPAY.

The repository may contain only:

- public CA certificates if repository policy explicitly permits them;
- non-secret trust metadata;
- configuration keys/placeholders.

Private keys, bind passwords, client secrets and production credentials are
forbidden in Git.

## 6. Bind/authentication strategy

La Régionale must identify the approved bind model.

Possible categories for classification only:

```text
service-account search + user bind
direct user bind
other bank-approved LDAP authentication flow
```

AUTH-1 does not choose one without bank evidence.

| Item | Required | Bank value/status |
|---|---:|---|
| Bind mechanism | YES | `TO_BE_CONFIRMED` |
| Service account required | YES | `TO_BE_CONFIRMED` |
| Service account identifier format | IF_APPLICABLE | `TO_BE_CONFIRMED` |
| Service account least-privilege requirements | IF_APPLICABLE | `TO_BE_CONFIRMED` |
| Anonymous search allowed | YES | `TO_BE_CONFIRMED` |
| User re-bind required for password verification | YES | `TO_BE_CONFIRMED` |
| SASL/simple bind/other | YES | `TO_BE_CONFIRMED` |

AUTH-1 stores no service-account password.

## 7. Secret ownership and rotation

If a service account or other credential is required:

```text
secret value
    -> approved secret-management system
    -> runtime injection
    -> never Git
```

The bank/SIXPAY teams must approve:

| Item | Required | Bank value/status |
|---|---:|---|
| Secret system of record | YES | `TO_BE_CONFIRMED` |
| Secret owner | YES | `TO_BE_CONFIRMED` |
| Retrieval/injection mechanism | YES | `TO_BE_CONFIRMED` |
| Rotation frequency/policy | YES | `TO_BE_CONFIRMED` |
| Rotation without application rebuild | YES | `TO_BE_CONFIRMED` |
| Emergency credential-revocation procedure | YES | `TO_BE_CONFIRMED` |

AUTH-1 does not assume Vault unless La Régionale explicitly approves it.

## 8. Directory search contract

### 8.1 Base/search scope

| Item | Required | Bank value/status |
|---|---:|---|
| Base DN | YES | `TO_BE_CONFIRMED` |
| User search base | YES | `TO_BE_CONFIRMED` |
| Search scope | YES | `TO_BE_CONFIRMED` |
| User search filter | YES | `TO_BE_CONFIRMED` |
| Escaping/injection protection requirements | YES | `APPROVED BY SIXPAY BASELINE` |
| Expected maximum result count for login search | YES | `TO_BE_CONFIRMED` |

User lookup must resolve exactly one intended human identity. Ambiguous results
must fail closed.

### 8.2 Login input attributes

| Item | Required | Bank value/status |
|---|---:|---|
| Human login attribute(s) | YES | `TO_BE_CONFIRMED` |
| Case-sensitivity rule | YES | `TO_BE_CONFIRMED` |
| Normalization rule | YES | `TO_BE_CONFIRMED` |
| Display-name attribute | NO | `TO_BE_CONFIRMED` |
| Username/display attribute used by SIXPAY | YES | `TO_BE_CONFIRMED` |

AUTH-1 does not assume `uid`, `cn`, `mail`, `sAMAccountName` or
`userPrincipalName`.

## 9. Canonical LDAP external identity contract

AUTH-0 froze the provider-neutral identity shape:

```text
ExternalIdentity(
    issuer,
    subject,
    username
)
```

AUTH-1 must obtain the concrete bank-approved mapping.

### 9.1 Stable subject

| Item | Required | Bank value/status |
|---|---:|---|
| Stable immutable LDAP attribute used as `subject` | YES | `TO_BE_CONFIRMED` |
| Attribute persistence across rename/move | YES | `TO_BE_CONFIRMED` |
| Attribute uniqueness scope | YES | `TO_BE_CONFIRMED` |
| Attribute exposure/read permission | YES | `TO_BE_CONFIRMED` |

The stable subject must not silently change when:

- the user's display name changes;
- the login name changes;
- the user is moved between organizational units;
- the email address changes.

DN must not be selected as the stable subject unless La Régionale explicitly
confirms its stability characteristics.

### 9.2 Logical trust-domain / issuer

SIXPAY needs a stable logical identifier to distinguish current and future
directories.

| Item | Required | Bank value/status |
|---|---:|---|
| LDAP logical trust-domain identifier | YES | `TO_BE_CONFIRMED` |
| Stability across server failover | YES | `TO_BE_CONFIRMED` |
| Stability across endpoint/DNS change | YES | `TO_BE_CONFIRMED` |
| Uniqueness across multiple directories | YES | `TO_BE_CONFIRMED` |

This logical identifier becomes the LDAP `ExternalIdentity.issuer` /
`security_user_identities.provider` value.

It is **not** necessarily the physical LDAP URL.

## 10. Authorization boundary

LDAP groups do not become SIXPAY authorities.

```text
LDAP groups
    -> informational/provisioning input only if separately approved

SIXPAY
    -> security_user_roles
    -> security_user_permissions
    -> final business authorization
```

AUTH-1 must record whether group membership is even readable/needed for future
administrative provisioning, but that information does not authorize runtime
group-to-role mapping.

| Item | Required | Bank value/status |
|---|---:|---|
| LDAP group information exposed | NO | `TO_BE_CONFIRMED` |
| Group attribute/search model | IF_NEEDED_LATER | `TO_BE_CONFIRMED` |
| Automatic group -> SIXPAY authority mapping | YES | `FORBIDDEN` |

## 11. LDAP account state semantics

La Régionale must document what directory state is visible and authoritative.

| Item | Required | Bank value/status |
|---|---:|---|
| Disabled account detection | YES | `TO_BE_CONFIRMED` |
| Locked account detection | YES | `TO_BE_CONFIRMED` |
| Password-expired state exposed | YES | `TO_BE_CONFIRMED` |
| Password-must-change state exposed | NO/IF_AVAILABLE | `TO_BE_CONFIRMED` |
| Expired account semantics | YES | `TO_BE_CONFIRMED` |
| Authentication error distinctions safe to expose to SIXPAY | YES | `TO_BE_CONFIRMED` |

SIXPAY must fail closed when LDAP authentication cannot establish a valid,
enabled identity.

LDAP password lifecycle remains directory-owned. SIXPAY must not reuse LOCAL
password-change/reset logic for LDAP credentials.

## 12. Timeout, resilience and failover

AUTH-1 requires explicit operational values.

| Item | Required | Bank value/status |
|---|---:|---|
| Connect timeout | YES | `TO_BE_CONFIRMED` |
| Read/operation timeout | YES | `TO_BE_CONFIRMED` |
| Authentication overall timeout budget | YES | `TO_BE_CONFIRMED` |
| Retry allowed | YES | `TO_BE_CONFIRMED` |
| Retry count/backoff | IF_ALLOWED | `TO_BE_CONFIRMED` |
| Multi-server/failover behavior | IF_AVAILABLE | `TO_BE_CONFIRMED` |
| Server selection order | IF_MULTI_SERVER | `TO_BE_CONFIRMED` |
| Health-check expectations | YES | `TO_BE_CONFIRMED` |
| Directory-unavailable behavior | YES | `FAIL CLOSED` |

Authentication retries must not create credential spraying or lockout
amplification.

No retry policy is implemented until AUTH-1 is approved.

## 13. Network reachability

La Régionale owns confirmation that SIXPAY runtime hosts can reach the LDAP
service.

| Item | Required | Bank value/status |
|---|---:|---|
| Source SIXPAY subnet/host scope | YES | `TO_BE_CONFIRMED` |
| Destination LDAP host(s) | YES | `TO_BE_CONFIRMED` |
| Destination port(s) | YES | `TO_BE_CONFIRMED` |
| Firewall opening approved | YES | `TO_BE_CONFIRMED` |
| Routing/VPN/private-network path | YES | `TO_BE_CONFIRMED` |
| DNS resolution validated from SIXPAY runtime | YES | `TO_BE_CONFIRMED` |
| TLS handshake validated from SIXPAY runtime | YES | `TO_BE_CONFIRMED` |

Network availability from a developer workstation is not sufficient evidence.

## 14. Audit and sensitive-data handling

LDAP authentication audit must follow the existing Security audit model.

Permitted audit data may include:

- canonical SIXPAY subject after successful linking;
- sanitized username/login identifier;
- trust-domain identifier;
- outcome;
- timestamp;
- correlation identifier where available.

Forbidden audit/log data includes:

- user passwords;
- service-account passwords;
- bind credentials;
- raw secret values;
- authentication tokens;
- private keys.

The bank must identify any additional restrictions for directory identifiers or
personal data.

## 15. Configuration ownership

### Security

Security owns semantics and validation of future configuration such as:

```text
sixpay.security.authentication.ldap.*
```

Exact property names are deferred until AUTH-1 approval/runtime implementation.

### Bootstrap

Bootstrap supplies environment-specific values and secret references only.
Bootstrap contains no LDAP authentication business logic.

### Frontend

Frontend does not require LDAP host, base DN, bind information or certificate
configuration. It only needs eventual backend-provided authentication
capability/presentation information.

## 16. Contract approval matrix

AUTH-1 must be reviewed by La Régionale and SIXGEN/SIXPAY engineering.

| Area | La Régionale | SIXPAY | Status |
|---|---|---|---|
| Directory endpoints | REQUIRED | REVIEW | `PENDING` |
| TLS/trust chain | REQUIRED | REVIEW | `PENDING` |
| Bind strategy | REQUIRED | REVIEW | `PENDING` |
| Search base/filter | REQUIRED | REVIEW | `PENDING` |
| Stable subject attribute | REQUIRED | REVIEW | `PENDING` |
| Logical trust-domain | REQUIRED | REVIEW | `PENDING` |
| Account-state semantics | REQUIRED | REVIEW | `PENDING` |
| Timeout/failover | REQUIRED | REVIEW | `PENDING` |
| Secret rotation | REQUIRED | REVIEW | `PENDING` |
| Network reachability | REQUIRED | REVIEW | `PENDING` |
| No group -> authority mapping | ACKNOWLEDGE | OWNER | `PENDING` |

## 17. Human gate

AUTH-1 cannot transition to `APPROVED` based only on code review, automated
tests or AI analysis.

Required evidence:

```text
La Régionale technical/security representative approval
+
SIXPAY security/architecture approval
```

Until then:

```text
AUTH-1 = PREPARED_PENDING_BANK_APPROVAL
LDAP runtime implementation = BLOCKED_FOR_PRODUCTION_CONTRACT
```

Development spikes using non-production mocks may be prepared separately, but
must not be presented as an approved bank integration.

## 18. No-secret rule

The following must never be committed:

```text
LDAP bind password
service-account password
private key
client certificate private key
production credential
secret-management token
```

Repository configuration may contain only non-secret placeholders/reference
keys.

## 19. AUTH-1 exit criteria

```text
[ ] LDAP/LDAPS URLs approved
[ ] TLS mode approved
[ ] CA/trust chain approved
[ ] bind mechanism approved
[ ] service-account model approved or marked N/A
[ ] Base DN approved
[ ] user search base/filter approved
[ ] stable subject attribute approved
[ ] username/display attribute approved
[ ] logical trust-domain approved
[ ] timeout values approved
[ ] multi-server/failover behavior approved or marked N/A
[ ] secret ownership/rotation approved
[ ] account lock/password-expiry semantics approved
[ ] runtime network reachability validated
[ ] no secrets committed
[ ] La Régionale human gate approved
[ ] SIXPAY architecture/security review approved
```

Only then may AUTH-1 be classified:

```text
AUTH-1 — APPROVED
```

## 20. Next lot

After AUTH-1 approval:

```text
AUTH-2 — LDAP Authentication Adapter
```

AUTH-2 may implement the provider-neutral LDAP adapter using the approved
connectivity, trust, search and stable-identity rules from this contract.
