# SIXPAY CONNECT — Security Golden Test Coverage

## Current authentication baseline

```text
Hybrid Authentication — LOCAL + OIDC + LDAP
Canonical SIXPAY principal and backend session
SIXPAY-owned roles and permissions
LOCAL-only SIXPAY password lifecycle
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
LDAP authentication boundary   COVERED
LDAP failure non-disclosure    COVERED
Hybrid provider coexistence    COVERED
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

Overall static coverage:

```text
SECURITY                              = COVERED
HYBRID AUTHENTICATION LOCAL/OIDC/LDAP = COVERED
```

Final closure still requires successful execution of the applicable validation
commands; this document does not manufacture execution evidence.

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
LdapAuthenticationFailureIT
LdapCanonicalAuthenticationServiceTest
ActiveDirectoryLdapAuthenticationAdapterTest
SecurityAuthorizationBoundaryIT
```

Critical regression evidence:

```text
SixpaySecurityAutoConfigurationTest
AuditingAuthenticationEntryPointTest
```

Covered behavior:

```text
LOCAL/OIDC/LDAP capability matrix
LOCAL session lifecycle
OIDC Bearer authentication
LDAP canonical session lifecycle
LDAP logout and /auth/me
generic LDAP 401 for invalid credentials, unlinked identity and disabled account
external identity linking boundary
disabled-user rejection
OIDC success/failure audit
three-provider coexistence
canonical authorization convergence
provider role/scope/group isolation
role/permission authorization
Angular session CSRF
Bearer CSRF boundary
LOCAL-only password lifecycle
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

Final feature status after all applicable validation commands are green:

```text
HYBRID AUTHENTICATION — LOCAL + OIDC + LDAP = CLOSED
```
