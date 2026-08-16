# SIXPAY CONNECT — Security Golden Test Coverage

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.8 — Security
Dual Authentication — Local + OIDC
DA-10.7 — Password lifecycle tests and audit closure
```

## Golden reference

`partner` remains the golden business-module reference.

Security is a sibling platform module and keeps its own focused evidence under:

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
Core model / policies         COVERED
LOCAL authentication          COVERED
OIDC authentication boundary  COVERED
Authorization                 COVERED
HTTP security infrastructure  COVERED
Identity linking              COVERED
Administration security       COVERED
Password lifecycle            COVERED
Security audit                COVERED
```

Overall:

```text
SECURITY = COVERED
```

## DA-10 Password lifecycle evidence

### Domain / policy

```text
PasswordPolicyTest
LocalCredentialTest
```

Evidence:

```text
configured min/max length
valid/invalid policy definition
temporary credential state
administrative reset state
user-owned change state
expiration timestamp
expiration boundary
```

### Application

```text
LocalAuthenticationServiceTest
LocalPasswordChangeServiceTest
SecurityUserAdministrationPasswordResetTest
```

Evidence:

```text
temporary/expired password authenticates into restricted session
normal password authenticates into normal session
current-password proof
central password policy
anti-reuse against current credential
anti-reuse against recent history
archive-before-replace ordering
separation of user change and ADMIN reset
PASSWORD_CHANGED audit
PASSWORD_RESET audit
```

### Password history infrastructure

```text
PasswordHistoryTest
```

Evidence:

```text
current credential replacement boundary
configured history window
history retention/pruning
missing LOCAL credential fails closed
```

### Integration

```text
LocalAuthenticationPasswordLifecycleIT
PasswordChangeControllerIT
```

Evidence:

```text
temporary login -> restricted principal
user password change -> lifecycle promotion
history archival
new credential login -> unrestricted principal
LOCAL-only password change boundary
same-session promotion after successful change
OIDC exclusion
request validation
```

### Frontend

```text
password-change.component.spec.ts
authentication.service.spec.ts
authentication.guard.spec.ts
```

Evidence:

```text
password form validation
backend error rendering
mandatory lifecycle submission
canonical session refresh
dashboard navigation after remediation
anonymous return URL
restricted LOCAL route enforcement
OIDC exclusion
```

## Audit

DA-10 extends the existing security operational audit rather than creating a
parallel audit model.

Required operational events:

```text
PASSWORD_RESET
PASSWORD_CHANGED
```

Passwords and password hashes are never audit payloads.

## Canonical closure document

See:

```text
backend/security/DA-10-PASSWORD-LIFECYCLE-CLOSURE.md
```

## Validation

Focused:

```bash
mvn -pl security \
  -Dtest=PasswordPolicyTest,LocalCredentialTest,LocalAuthenticationServiceTest,LocalPasswordChangeServiceTest,PasswordHistoryTest,SecurityUserAdministrationPasswordResetTest,LocalPasswordControllerTest \
  test
```

Integration:

```bash
mvn -pl security \
  -Pfull-tests \
  -Dit.test=LocalAuthenticationPasswordLifecycleIT,PasswordChangeControllerIT \
  verify
```

Module:

```bash
mvn -pl security -am test
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

Golden gate:

```bash
cd ../backend
mvn -pl tests \
  -Dtest=BackendGoldenCoverageGateTest \
  test
```

## Exit decision

```text
DA-10 PASSWORD LIFECYCLE = COVERED
SECURITY                 = COVERED
```
