# SIXPAY CONNECT — DA-10 Password Lifecycle Closure

## Scope

```text
Dual Authentication — Local + OIDC
DA-10 — Password lifecycle
DA-10.1 — Password policy domain
DA-10.2 — Credential lifecycle model
DA-10.3 — Password history
DA-10.4 — Change/reset use cases
DA-10.5 — Authentication enforcement
DA-10.6 — Frontend lifecycle
DA-10.7 — Tests and audit
```

## Architectural decision

Password lifecycle is owned only by SIXPAY LOCAL authentication.

```text
LOCAL -> SIXPAY credential lifecycle
OIDC  -> lifecycle owned by IdP
```

A valid LOCAL password may authenticate a user while still producing a
restricted session when the credential is temporary or expired.

Restricted sessions allow only the authentication lifecycle required to
complete remediation.

## Golden evidence

### Policy

`PasswordPolicyTest`

Proves configurable minimum/maximum length and validates policy definition.

### Credential lifecycle

`LocalCredentialTest`

Proves:

```text
administrative provisioning -> mustChangePassword=true
administrative reset        -> mustChangePassword=true
user password change        -> mustChangePassword=false
passwordChangedAt           -> populated
expiresAt                   -> policy-derived
expiration boundary         -> enforced
```

`LocalAuthenticationServiceTest`

Proves valid temporary and expired credentials produce an authenticated but
restricted principal, while a normal credential produces a normal principal.

### Change and anti-reuse

`LocalPasswordChangeServiceTest`

Proves:

```text
current password proof precedes policy/history evaluation
current password cannot be reused
recent password cannot be reused
old hash is archived before replacement
PASSWORD_CHANGED audit is produced
```

`PasswordHistoryTest`

Proves:

```text
current credential is loaded under the replacement boundary
only configured recent hashes are exposed
history-size zero is supported
replaced hashes are archived
history rows beyond configured retention are pruned
missing LOCAL credential fails closed
```

### Administrative reset

`SecurityUserAdministrationPasswordResetTest`

Proves the ADMIN workflow remains separate from user-owned change:

```text
policy -> anti-reuse -> archive -> temporary credential -> PASSWORD_RESET audit
```

The reset never promotes the user's session and the resulting credential
remains a must-change credential through the persistence adapter.

### Authentication enforcement

`LocalAuthenticationPasswordLifecycleIT`

Composes the real password lifecycle domain/application services around one
shared in-memory security boundary:

```text
temporary password login
-> authenticated + restricted
-> user password change
-> history archive
-> credential lifecycle promotion
-> second login
-> authenticated + unrestricted
```

### HTTP boundary

`LocalPasswordControllerTest` and `PasswordChangeControllerIT`

Prove:

```text
LOCAL session can change its own password
canonical user id is taken from authenticated SIXPAY subject
successful change promotes the same session
OIDC session cannot use SIXPAY LOCAL password lifecycle
invalid request is rejected before use-case execution
```

### Frontend

`password-change.component.spec.ts`

Proves:

```text
confirmation must match
minimum length is enforced
current/new password are submitted to AuthenticationService
backend lifecycle rejection detail is displayed
```

`authentication.service.spec.ts`

Proves:

```text
successful mandatory password change reloads canonical /auth/me session
passwordChangeRequired becomes false
stale authentication return URL is cleared
navigation terminates on dashboard
```

`authentication.guard.spec.ts`

Proves:

```text
anonymous -> /login?returnUrl=...
restricted LOCAL -> /change-password
normal LOCAL -> business route allowed
OIDC -> not subject to SIXPAY LOCAL password restriction
```

## Audit evidence

DA-10 uses the existing security audit stream.

Required events:

```text
PASSWORD_RESET   administrative temporary credential reset
PASSWORD_CHANGED user-owned successful credential change
```

The database check constraint is synchronized with
`SecurityAuditEventType`, including DA-10 audit events.

No raw password or password hash is written to the audit event.

## Validation

Backend focused unit tests:

```bash
cd backend

mvn -pl security \
  -Dtest=PasswordPolicyTest,LocalCredentialTest,LocalAuthenticationServiceTest,LocalPasswordChangeServiceTest,PasswordHistoryTest,SecurityUserAdministrationPasswordResetTest,LocalPasswordControllerTest \
  test
```

Backend integration tests:

```bash
mvn -pl security \
  -Pfull-tests \
  -Dit.test=LocalAuthenticationPasswordLifecycleIT,PasswordChangeControllerIT \
  verify
```

Full backend:

```bash
mvn clean package
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

DA-10 is closed when:

```text
policy tests                  GREEN
credential lifecycle tests    GREEN
password history tests        GREEN
administrative reset tests    GREEN
authentication lifecycle IT   GREEN
password-change controller IT GREEN
frontend lifecycle tests      GREEN
backend full-tests             GREEN
frontend build                 GREEN
golden coverage gate           GREEN
```

Classification:

```text
DA-10 PASSWORD LIFECYCLE = COVERED
```
