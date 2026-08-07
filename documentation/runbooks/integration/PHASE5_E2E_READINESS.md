# Phase 5 E2E readiness runbook

## 1. Deterministic validation

From `backend/`:

```bash
mvn -U clean verify
```

Then run bootstrap readiness guards:

```bash
mvn -U -pl bootstrap -am \
  -Dtest='com.sixpay.bootstrap.readiness.*' test
```

On Windows PowerShell use the provided:

```powershell
scripts\readiness\phase5-readiness.ps1
```

## 2. What a green local gate means

A green deterministic gate means:

- the modular monolith compiles;
- module and architecture tests are green;
- documented Phase 5 boundaries are present;
- no external sandbox success is being fabricated.

It does not mean production providers have been certified.

## 3. External certification inputs

Before running provider certification, obtain:

- sandbox endpoint;
- client/token identity;
- scopes/audience;
- mTLS client certificate and private-key delivery process;
- trust chain/CA;
- test accounts and expected balances/statuses;
- permitted failure cases;
- provider support contact;
- approved test window.

Never place these secrets in the repository.

## 4. Go/no-go review

A production go decision requires:

1. deterministic gate PASS;
2. all required external sandbox scenarios PASS;
3. unresolved `BLOCKED_EXTERNAL` items explicitly waived or resolved;
4. dashboards and alert thresholds assigned;
5. runbook owners assigned;
6. rollback/compensation procedure rehearsed;
7. Security approval for secrets, certificates and RBAC;
8. Operations approval for monitoring and replay.

## 5. Incident during certification

Capture only:

- scenario ID;
- timestamp;
- correlation/event/request ID;
- safe provider error code;
- HTTP status;
- masked endpoint/environment identifier.

Do not attach:

- access token;
- API key;
- private key;
- raw account/customer data;
- unmasked credentials.

## 6. Failure after financial command

Never rerun a reservation/posting/reversal merely because the client timed out.

Use the relevant lookup/reconciliation operation first.

## 7. Failure after outbox commit

Do not recreate the business transaction.

Restart the relay/consumer and verify:

- event/outbox entry still exists;
- claim/retry state is safe;
- consumer idempotency blocks duplicate functional effects.

## 8. Notification DLQ

Fix the provider/configuration issue before replaying a `DEAD_LETTERED`
notification. Replay the same notification identity and preserve audit history.
