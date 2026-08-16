# SIXPAY CONNECT — DA-12 Dual Authentication Final Closure

## Scope

```text
Dual Authentication — Local + OIDC
DA-12 — Documentation + validation gate
```

DA-12 does not introduce a new authentication mechanism or new production
behavior. It closes the feature by consolidating the architecture decisions,
operational rules, test evidence and validation gates already implemented in
DA-1 through DA-11.

## Source of truth

```text
ENGINEERING_CONTEXT.md
feat/hybrid-authentification-system
```

The `partner` module remains the golden business-module reference. The
`security` module is a platform module and keeps its focused golden evidence
under:

```text
backend/security/
```

## Final authentication model

SIXPAY exposes two independent authentication capabilities:

```text
LOCAL
OIDC
```

Hybrid means:

```text
LOCAL enabled
+
OIDC enabled
```

It is not a third authentication mode.

## Canonical principal

Both authentication mechanisms converge to the canonical SIXPAY principal:

```text
AuthenticatedUser
├── subject
├── username
├── roles
├── permissions
└── passwordChangeRequired
```

The authentication mechanism establishes identity. SIXPAY owns authorization.

## Authorization ownership

```text
OIDC IdP
  -> authenticates external identity
  -> supplies issuer / subject / identity claims

SIXPAY
  -> resolves linked canonical account
  -> owns roles
  -> owns permissions
  -> authorizes business operations
```

Provider roles, groups and scopes never become SIXPAY business authorities
directly.

## LOCAL password lifecycle

LOCAL password lifecycle is owned by SIXPAY:

```text
policy
history
expiration
mustChangePassword
administrative reset
user-owned change
restricted session
audit
```

A valid LOCAL credential may authenticate while still producing a restricted
session when remediation is required.

Restricted LOCAL sessions allow only the lifecycle operations required for
remediation.

## OIDC password lifecycle

OIDC password lifecycle is owned by the IdP.

SIXPAY does not apply the LOCAL password lifecycle to OIDC authentication and
does not expose a LOCAL `passwordChangeRequired` restriction for an OIDC
session.

## Session model

LOCAL and OIDC both converge to the mechanism-neutral SIXPAY session model.

```text
LOCAL login
  -> SIXPAY session
  -> /api/v1/auth/me

OIDC Bearer
  -> canonical SIXPAY principal
  -> optional backend OIDC session
  -> /api/v1/auth/me
```

## CSRF model

Cookie/session mutations remain CSRF protected.

```text
XSRF-TOKEN cookie
+
X-XSRF-TOKEN header
```

Bearer requests are excluded from session CSRF because the Bearer token is an
explicit request credential. Authentication and SIXPAY authorization still
apply normally.

## Audit model

Dual Authentication reuses the existing operational security audit.

Important authentication/lifecycle events include:

```text
OIDC_LOGIN_SUCCESS
OIDC_LOGIN_FAILURE
PASSWORD_RESET
PASSWORD_CHANGED
```

Raw passwords and password hashes are never audit payloads.

## Golden evidence

### DA-10 — Password lifecycle

Canonical documentation:

```text
DA-10-PASSWORD-LIFECYCLE-CLOSURE.md
```

### DA-11 — Integration/security tests

Canonical documentation:

```text
DA-11-INTEGRATION-SECURITY-CLOSURE.md
```

Primary integration evidence:

```text
AuthenticationCapabilityMatrixIT
LocalAuthenticationSessionIT
OidcAuthenticationProviderIT
HybridAuthenticationIT
SecurityAuthorizationBoundaryIT
```

Critical regression evidence:

```text
SixpaySecurityAutoConfigurationTest
AuditingAuthenticationEntryPointTest
```

### DA-12 — Documentation + validation gate

Canonical gate:

```text
DualAuthenticationGoldenGateTest
```

The gate verifies that the DA-11 evidence and the final DA-12 documentation
remain present and synchronized. It does not duplicate behavioral tests.

## Validation gate

### 1. Documentation/golden gate

From `backend`:

```bash
mvn -pl security \
  -Dtest=DualAuthenticationGoldenGateTest \
  test
```

### 2. Critical security regression tests

```bash
mvn -pl security \
  -Dtest=SixpaySecurityAutoConfigurationTest,AuditingAuthenticationEntryPointTest,DualAuthenticationGoldenGateTest \
  test
```

### 3. Focused DA-11 integration/security gate

```bash
mvn -pl security \
  -Pfull-tests \
  -Dit.test=AuthenticationCapabilityMatrixIT,LocalAuthenticationSessionIT,OidcAuthenticationProviderIT,HybridAuthenticationIT,SecurityAuthorizationBoundaryIT \
  verify
```

### 4. Complete security module

```bash
mvn -pl security \
  -Pfull-tests \
  clean verify
```

### 5. Complete backend

```bash
mvn \
  -Pfull-tests \
  clean verify
```

### 6. Frontend

From `frontend`:

```bash
npm test
npm run build
```

The frontend validation is required because the Dual Authentication feature
also owns login bootstrap, mandatory password-change navigation and canonical
session presentation.

## Final exit criteria

```text
LOCAL capability                                  GREEN
OIDC capability                                   GREEN
hybrid coexistence                                GREEN
canonical principal convergence                   GREEN
SIXPAY-owned authorization                        GREEN
LOCAL password lifecycle                          GREEN
OIDC lifecycle isolation                          GREEN
session behavior                                  GREEN
restricted LOCAL session                          GREEN
CSRF session boundary                             GREEN
Bearer security boundary                          GREEN
authentication/lifecycle audit                    GREEN
DA-11 integration/security evidence               GREEN
DA-12 documentation gate                          GREEN
security module full-tests                        GREEN
backend full-tests                                GREEN
frontend tests/build                              GREEN
```

Classification:

```text
DA-12 DOCUMENTATION + VALIDATION GATE = COVERED
DUAL AUTHENTICATION — LOCAL + OIDC = CLOSED
```
