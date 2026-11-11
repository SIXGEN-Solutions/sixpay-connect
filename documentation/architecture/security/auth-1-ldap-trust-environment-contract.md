# SIXPAY CONNECT — AUTH-1
## LDAP Trust & Environment Contract

### Status

```text
AUTH-1 — APPROVED BASELINE FOR MICROSOFT ACTIVE DIRECTORY
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
| Primary directory URL | YES | `CONFIGURABLE_PER_ENVIRONMENT` |
| Secondary/failover directory URL(s) | NO/IF_AVAILABLE | `CONFIGURABLE_LIST`; currently optional pending bank topology |
| Protocol | YES | `LDAPS` |
| Port(s) | YES | `CONFIGURABLE`; standard LDAPS default may be supplied by deployment configuration |
| Environment scope | YES | `CONFIGURABLE_PER_ENVIRONMENT` |
| DNS ownership/resolution requirements | YES | `BANK_MANAGED / RUNTIME_RESOLUTION_REQUIRED` |

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
| TLS/LDAPS mandatory | YES | `APPROVED` |
| LDAPS or StartTLS | YES | `LDAPS` |
| Minimum TLS version | YES | `CONFIGURABLE`; bank/runtime policy applies |
| Server certificate hostname/SAN expectation | YES | `REQUIRED` |
| Private/public CA model | YES | `TRUSTED_CA_REQUIRED` |
| CA chain delivery mechanism | YES | `CONFIGURABLE_TRUSTSTORE / RUNTIME_TRUST_MATERIAL` |
| Certificate expiry/renewal ownership | YES | `BANK/INFRASTRUCTURE_OWNED` |

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
| Bind mechanism | YES | `SERVICE_ACCOUNT_SEARCH + USER_CREDENTIAL_VERIFICATION` |
| Service account required | YES | `YES` |
| Service account identifier format | IF_APPLICABLE | `CONFIGURABLE` |
| Service account least-privilege requirements | IF_APPLICABLE | `READ_ONLY_USER_SEARCH` |
| Anonymous search allowed | YES | `NO` |
| User re-bind required for password verification | YES | `YES` |
| SASL/simple bind/other | YES | `CONFIGURABLE_ACTIVE_DIRECTORY_BIND_MODE`; no anonymous bind |

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
| Secret system of record | YES | `RUNTIME_SECRET_SOURCE`; concrete product deferred |
| Secret owner | YES | `BANK/SIXPAY_OPERATIONS` |
| Retrieval/injection mechanism | YES | `RUNTIME_INJECTION`; never Git |
| Rotation frequency/policy | YES | `IMPROVEMENT_PROPOSAL_PENDING`; capability must allow replacement without code change |
| Rotation without application rebuild | YES | `TARGET_REQUIRED`; implementation deferred |
| Emergency credential-revocation procedure | YES | `OPERATIONS_PROCEDURE_TO_DEFINE` |

AUTH-1 does not assume Vault unless La Régionale explicitly approves it.

## 8. Directory search contract

### 8.1 Base/search scope

| Item | Required | Bank value/status |
|---|---:|---|
| Base DN | YES | `CONFIGURABLE` |
| User search base | YES | `CONFIGURABLE`; prefer the narrowest OU subtree containing eligible human users |
| Search scope | YES | `SUBTREE` within configured user search base |
| User search filter | YES | `CONFIGURABLE`; default semantic `(sAMAccountName={0})`, escaped parameter, exactly one match required |
| Escaping/injection protection requirements | YES | `APPROVED BY SIXPAY BASELINE` |
| Expected maximum result count for login search | YES | `1` |

User lookup must resolve exactly one intended human identity. Ambiguous results
must fail closed.

### 8.2 Login input attributes

| Item | Required | Bank value/status |
|---|---:|---|
| Human login attribute(s) | YES | `sAMAccountName` default, configurable |
| Case-sensitivity rule | YES | `CASE_INSENSITIVE_NORMALIZATION` |
| Normalization rule | YES | `TRIM + LOWERCASE_FOR_LOOKUP_INPUT`; original directory value retained for display |
| Display-name attribute | NO | `displayName` default, configurable |
| Username/display attribute used by SIXPAY | YES | `sAMAccountName` default, configurable |

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
| Stable immutable LDAP attribute used as `subject` | YES | `objectGUID` default for Active Directory, configurable |
| Attribute persistence across rename/move | YES | `REQUIRED`; `objectGUID` selected for this property |
| Attribute uniqueness scope | YES | `DIRECTORY/TRUST_DOMAIN` |
| Attribute exposure/read permission | YES | `REQUIRED_FOR_SERVICE_ACCOUNT` |

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
| LDAP logical trust-domain identifier | YES | `regionale-ldap` default, configurable |
| Stability across server failover | YES | `REQUIRED` |
| Stability across endpoint/DNS change | YES | `REQUIRED` |
| Uniqueness across multiple directories | YES | `REQUIRED` |

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
| LDAP group information exposed | NO | `NOT_REQUIRED_FOR_AUTHENTICATION` |
| Group attribute/search model | IF_NEEDED_LATER | `DEFERRED` |
| Automatic group -> SIXPAY authority mapping | YES | `FORBIDDEN` |

## 11. LDAP account state semantics

La Régionale must document what directory state is visible and authoritative.

| Item | Required | Bank value/status |
|---|---:|---|
| Disabled account detection | YES | `ENABLED_WHEN_EXPOSED_BY_AD`; fail closed |
| Locked account detection | YES | `ENABLED_WHEN_EXPOSED_BY_AD`; fail closed |
| Password-expired state exposed | YES | `SUPPORTED_WHEN_EXPOSED_BY_AD`; configurable mapping |
| Password-must-change state exposed | NO/IF_AVAILABLE | `SUPPORTED_WHEN_EXPOSED_BY_AD`; configurable mapping |
| Expired account semantics | YES | `FAIL_CLOSED` |
| Authentication error distinctions safe to expose to SIXPAY | YES | `INTERNAL_CLASSIFICATION_ONLY`; external response remains generic |

SIXPAY must fail closed when LDAP authentication cannot establish a valid,
enabled identity.

LDAP password lifecycle remains directory-owned. SIXPAY must not reuse LOCAL
password-change/reset logic for LDAP credentials.

## 12. Timeout, resilience and failover

AUTH-1 requires explicit operational values.

| Item | Required | Bank value/status |
|---|---:|---|
| Connect timeout | YES | `3s` default, configurable |
| Read/operation timeout | YES | `5s` default, configurable |
| Authentication overall timeout budget | YES | `10s` default, configurable |
| Retry allowed | YES | `ONLY_ACROSS_CONFIGURED_ALTERNATE_ENDPOINTS`; no credential spraying |
| Retry count/backoff | IF_ALLOWED | `BOUNDED_BY_ENDPOINT_COUNT`; configurable backoff |
| Multi-server/failover behavior | IF_AVAILABLE | `CONFIGURABLE_ORDERED_ENDPOINT_LIST`; single endpoint allowed initially |
| Server selection order | IF_MULTI_SERVER | `CONFIGURATION_ORDER` |
| Health-check expectations | YES | `CONNECTIVITY/TLS_READINESS_TO_BE_EXPOSED_BY_RUNTIME` |
| Directory-unavailable behavior | YES | `FAIL CLOSED` |

Authentication retries must not create credential spraying or lockout
amplification.

No retry policy is implemented until AUTH-1 is approved.

## 13. Network reachability

La Régionale owns confirmation that SIXPAY runtime hosts can reach the LDAP
service.

| Item | Required | Bank value/status |
|---|---:|---|
| Source SIXPAY subnet/host scope | YES | `CONFIRMED_REACHABLE_BY_LA_REGIONALE` |
| Destination LDAP host(s) | YES | `CONFIGURABLE` |
| Destination port(s) | YES | `CONFIGURABLE` |
| Firewall opening approved | YES | `APPROVED/CONFIRMED` |
| Routing/VPN/private-network path | YES | `CONFIRMED_AVAILABLE`; exact topology environment-owned |
| DNS resolution validated from SIXPAY runtime | YES | `REQUIRED_BEFORE_ENVIRONMENT_GO_LIVE` |
| TLS handshake validated from SIXPAY runtime | YES | `REQUIRED_BEFORE_ENVIRONMENT_GO_LIVE` |

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
AUTH-1 = APPROVED_BASELINE
LDAP runtime implementation = AUTHORIZED_TO_PROCEED_WITH_APPROVED_BASELINE
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

Human decisions have now been supplied for the Microsoft Active Directory baseline.
AUTH-1 is classified:

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


## 21. Approved Microsoft Active Directory profile

The human-approved AUTH-1 profile is:

```text
directoryType            = MICROSOFT_ACTIVE_DIRECTORY
transport                = LDAPS
endpointUrls             = configurable per environment
baseDn                   = configurable
userSearchBase           = configurable, narrowest eligible human-user subtree preferred
userSearchFilter         = configurable; default semantic (sAMAccountName={0})
loginAttribute           = sAMAccountName (configurable)
displayAttribute         = displayName (configurable)
subjectAttribute         = objectGUID (configurable but mandatory)
trustDomain              = regionale-ldap (configurable)
serviceAccount           = required, read-only search privilege
userCredentialCheck      = required after unique lookup
connectTimeout           = 3s default, configurable
readTimeout              = 5s default, configurable
overallAuthentication    = 10s default, configurable
multiServer              = ordered configurable endpoint list
directoryUnavailable     = fail closed
groupToAuthorityMapping  = forbidden
secretStorage            = runtime injection; never Git
secretRotation           = improvement proposal deferred
```

`objectGUID` must be normalized to a stable textual representation before
constructing `ExternalIdentity.subject`. The full Distinguished Name may be
used transiently to perform the user bind but is not the canonical SIXPAY
external subject.

The human gate from La Régionale is recorded as approved for this baseline.
Environment-specific endpoint, DN, certificate and secret values remain runtime
configuration and are not committed.
