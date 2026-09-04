# SIXPAY CONNECT — DA-11 Integration / Security Golden Closure

## Scope

```text
Dual Authentication — Local + OIDC

DA-11 — Integration/security tests
DA-11.1 — Capability matrix
DA-11.2 — Local session integration
DA-11.3 — OIDC integration
DA-11.4 — Hybrid coexistence
DA-11.5 — Authorization + CSRF
DA-11.6 — Golden closure
```

## Repository rule

The authoritative implementation revision is selected through:

```text
TASK_INVOCATION_OR_EXECUTION_ENVIRONMENT
```

`ENGINEERING_CONTEXT.md` remains the mandatory repository entry point.

The `partner` module remains the golden business-module reference. Security is
a platform module and keeps its focused evidence under:

```text
backend/security/
```

The golden testing rule remains:

```text
one test = one responsibility
behavioral evidence > test-count inflation
```

## Architectural invariants locked by DA-11

### Authentication capabilities

SIXPAY supports two independent capabilities:

```text
LOCAL
OIDC
```

Hybrid means both capabilities are enabled simultaneously. It is not a third
authentication mechanism.

### Canonical principal

Both mechanisms converge to the canonical SIXPAY principal:

```text
AuthenticatedUser
├── subject
├── username
├── roles
├── permissions
└── passwordChangeRequired
```

### Authorization ownership

```text
IdP
  -> proves external identity

SIXPAY
  -> resolves canonical user
  -> owns roles
  -> owns permissions
  -> owns business authorization
```

OIDC provider roles, scopes or groups must never become SIXPAY business
authorities directly.

### Password lifecycle ownership

```text
LOCAL -> SIXPAY password lifecycle
OIDC  -> password lifecycle owned by IdP
```

`passwordChangeRequired` restricts LOCAL sessions only.

### CSRF boundary

```text
cookie/session mutation
  -> CSRF required

Bearer request
  -> explicit bearer credential
  -> session CSRF not required
  -> authentication + SIXPAY authorization still required
```

## DA-11.1 — Capability matrix

Primary evidence:

```text
AuthenticationCapabilityMatrixIT
```

Required matrix:

```text
LOCAL=false / OIDC=false
LOCAL=true  / OIDC=false
LOCAL=false / OIDC=true
LOCAL=true  / OIDC=true
```

Evidence:

```text
capability properties are bound correctly
LOCAL authentication boundary appears only when enabled
OIDC adapter appears only when enabled
LOCAL login mapping appears only when LOCAL is enabled
hybridEnabled is true only when both are enabled
```

## DA-11.2 — Local session integration

Primary evidence:

```text
LocalAuthenticationSessionIT
```

Evidence:

```text
LOCAL login creates canonical authenticated session
/auth/me exposes the same canonical principal
normal LOCAL session can access protected resources
session mutation without CSRF is rejected
Angular XSRF cookie/header contract is accepted
logout terminates authentication
temporary/expired LOCAL credential creates restricted session
restricted session can still reach /auth/me
restricted session cannot reach business resources
```

## DA-11.3 — OIDC integration

Primary evidence:

```text
OidcAuthenticationProviderIT
```

Regression evidence:

```text
AuditingAuthenticationEntryPointTest
```

Evidence:

```text
valid Bearer authentication resolves canonical SIXPAY identity
provider authorization is ignored
unlinked external identity is rejected
disabled canonical SIXPAY user is rejected
invalid JWT is rejected before identity resolution
required OIDC claims are enforced
Bearer failures preserve OAuth2 WWW-Authenticate semantics
Bearer authentication failures produce OIDC_LOGIN_FAILURE
anonymous 401 does not produce false OIDC_LOGIN_FAILURE
successful OIDC authentication produces OIDC_LOGIN_SUCCESS
```

## DA-11.4 — Hybrid coexistence

Primary evidence:

```text
HybridAuthenticationIT
```

Evidence:

```text
LOCAL remains available while OIDC is enabled
OIDC remains available while LOCAL is enabled
both mechanisms converge to the same SIXPAY authorization model
OIDC never exposes LOCAL password-change restriction
OIDC Bearer can be promoted to backend session
OIDC session creation does not disable LOCAL login
```

## DA-11.5 — Authorization + CSRF

Primary evidence:

```text
SecurityAuthorizationBoundaryIT
```

Regression evidence:

```text
SixpaySecurityAutoConfigurationTest
```

Evidence:

```text
anonymous protected request -> 401
authenticated principal missing required authority -> 403
required SIXPAY role -> authorized
required SIXPAY permission -> authorized
session mutation without CSRF -> 403
matching Angular XSRF cookie/header -> authorized
mismatched Angular XSRF cookie/header -> 403
Bearer mutation does not require session CSRF
provider role cannot cross SIXPAY authorization boundary
provider scope cannot cross SIXPAY authorization boundary
SIXPAY-resolved OIDC permission authorizes request
```

## DA-11.6 — Golden gate

Canonical gate:

```text
DualAuthenticationGoldenGateTest
```

The gate intentionally does not repeat behavioral assertions.

It verifies that the repository still contains:

```text
AuthenticationCapabilityMatrixIT
LocalAuthenticationSessionIT
OidcAuthenticationProviderIT
HybridAuthenticationIT
SecurityAuthorizationBoundaryIT
SixpaySecurityAutoConfigurationTest
AuditingAuthenticationEntryPointTest
DA-11-INTEGRATION-SECURITY-CLOSURE.md
```

Behavior remains owned by each focused test. Failsafe remains responsible for
executing `*IT` classes through the existing `full-tests` Maven profile.

## Validation

### Golden closure gate

From `backend/security`:

```bash
mvn \
  -Dtest=DualAuthenticationGoldenGateTest \
  test
```

Or from `backend`:

```bash
mvn -pl security \
  -Dtest=DualAuthenticationGoldenGateTest \
  test
```

### DA-11 focused integration gate

```bash
mvn -pl security \
  -Pfull-tests \
  -Dit.test=AuthenticationCapabilityMatrixIT,LocalAuthenticationSessionIT,OidcAuthenticationProviderIT,HybridAuthenticationIT,SecurityAuthorizationBoundaryIT \
  verify
```

### Critical unit regression gate

```bash
mvn -pl security \
  -Dtest=SixpaySecurityAutoConfigurationTest,AuditingAuthenticationEntryPointTest,DualAuthenticationGoldenGateTest \
  test
```

### Security module full gate

```bash
mvn -pl security \
  -Pfull-tests \
  clean verify
```

### Full backend gate

```bash
mvn \
  -Pfull-tests \
  clean verify
```

## Exit decision

DA-11 closes only when:

```text
capability matrix                         GREEN
LOCAL session integration                GREEN
OIDC integration                         GREEN
hybrid coexistence                       GREEN
authorization boundary                   GREEN
CSRF boundary                            GREEN
OIDC failure audit regression            GREEN
security auto-configuration regression   GREEN
DA-11 golden closure gate                GREEN
security module full-tests               GREEN
backend full-tests                       GREEN
```

Classification:

```text
DA-11 DUAL AUTHENTICATION INTEGRATION/SECURITY = COVERED
```
