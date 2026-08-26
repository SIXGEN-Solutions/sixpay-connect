# SIXPAY CONNECT — Security Golden Test Coverage

## Phase

```text
Dual Authentication — Local + OIDC
DA-10 — Password lifecycle
DA-11 — Integration/security tests
DA-12 — Documentation + validation gate
```

## Golden reference

`partner` remains the golden business-module reference.

Security is a sibling platform module and keeps its focused evidence under:

```text
backend/security/
```

The golden rule remains:

```text
one test = one responsibility
behavioral evidence > test-count inflation
```

## Final classification

```text
Core model / policies          COVERED
LOCAL authentication           COVERED
OIDC authentication boundary   COVERED
Hybrid coexistence             COVERED
Canonical principal            COVERED
Authorization                  COVERED
HTTP security infrastructure   COVERED
CSRF boundary                  COVERED
Identity linking               COVERED
Administration security        COVERED
Password lifecycle             COVERED
Security audit                 COVERED
Integration/security matrix    COVERED
Documentation validation       COVERED
```

Overall:

```text
SECURITY            = COVERED
DUAL AUTHENTICATION = COVERED
```

## DA-10 Password lifecycle evidence

Canonical closure:

```text
DA-10-PASSWORD-LIFECYCLE-CLOSURE.md
```

Key evidence:

```text
PasswordPolicyTest
LocalCredentialTest
LocalAuthenticationServiceTest
LocalPasswordChangeServiceTest
PasswordHistoryTest
SecurityUserAdministrationPasswordResetTest
LocalAuthenticationPasswordLifecycleIT
PasswordChangeControllerIT
```

Covered behavior:

```text
configurable password policy
credential lifecycle metadata
expiration
must-change state
password history
anti-reuse
administrative temporary reset
user-owned password change
restricted LOCAL session
same-session promotion
PASSWORD_RESET audit
PASSWORD_CHANGED audit
OIDC exclusion from LOCAL password lifecycle
```

## DA-11 Integration/security evidence

Canonical closure:

```text
DA-11-INTEGRATION-SECURITY-CLOSURE.md
```

Primary evidence:

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

Covered behavior:

```text
LOCAL/OIDC capability matrix
LOCAL session lifecycle
OIDC Bearer authentication
external identity linking boundary
disabled-user rejection
OIDC success/failure audit
hybrid coexistence
canonical authorization convergence
provider role/scope isolation
role/permission authorization
Angular session CSRF
Bearer CSRF boundary
```

## DA-12 Documentation + validation gate

Canonical final closure:

```text
DA-12-DUAL-AUTHENTICATION-CLOSURE.md
```

Golden gate:

```text
DualAuthenticationGoldenGateTest
```

DA-12 adds no production behavior. It synchronizes and protects:

```text
DA-10 closure documentation
DA-11 closure documentation
final Dual Authentication architecture decisions
golden evidence inventory
validation commands
final exit decision
```

## Validation

Documentation/golden gate:

```bash
cd backend

mvn -pl security \
  -Dtest=DualAuthenticationGoldenGateTest \
  test
```

Critical regression gate:

```bash
mvn -pl security \
  -Dtest=SixpaySecurityAutoConfigurationTest,AuditingAuthenticationEntryPointTest,DualAuthenticationGoldenGateTest \
  test
```

Focused integration gate:

```bash
mvn -pl security \
  -Pfull-tests \
  -Dit.test=AuthenticationCapabilityMatrixIT,LocalAuthenticationSessionIT,OidcAuthenticationProviderIT,HybridAuthenticationIT,SecurityAuthorizationBoundaryIT \
  verify
```

Security module:

```bash
mvn -pl security -Pfull-tests clean verify
```

Full backend:

```bash
mvn -Pfull-tests clean verify
```

Frontend:

```bash
cd ../frontend
npm test
npm run build
```

## Exit decision

```text
DA-10 PASSWORD LIFECYCLE                    = COVERED
DA-11 INTEGRATION / SECURITY                = COVERED
DA-12 DOCUMENTATION + VALIDATION GATE       = COVERED
SECURITY                                    = COVERED
DUAL AUTHENTICATION                         = COVERED
```

Final feature status after all validation commands are green:

```text
DUAL AUTHENTICATION — LOCAL + OIDC = CLOSED
```
