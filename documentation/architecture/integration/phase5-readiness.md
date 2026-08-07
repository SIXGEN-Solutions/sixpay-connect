# Phase 5 — End-to-end validation and readiness

## 1. Purpose

This document closes Phase 5 without overstating external-provider readiness.

SIXPAY CONNECT is delivered first as a modular monolith. Internal module
interactions remain in-process when the modules are co-deployed. Kafka remains
available for selected distributed integration cases and future decomposition,
but is not the default internal transport.

Two independent gates are therefore defined:

- `MODULAR_MONOLITH_READINESS`;
- `EXTERNAL_SANDBOX_CERTIFICATION`.

A build may pass the first gate while the second is still blocked.

## 2. Current readiness statement

```text
MODULAR_MONOLITH_READINESS = VERIFIABLE
EXTERNAL_SANDBOX_CERTIFICATION = PENDING_EXTERNAL_INPUTS
PRODUCTION_READINESS = NOT_ASSERTED
```

This lot does not hard-code or claim production readiness.

## 3. Phase 5 implementation inventory

| Area | Current implementation evidence | Local/CI readiness | External certification |
|---|---|---|---|
| Transverse integration foundation | correlation/request context, REST foundation, errors, retry policy, event envelope, Kafka/DLQ/idempotency support | testable | N/A |
| TresorPay inbound Payment | controller, security, idempotency, anti-replay, rate limiting, audit, callback foundation/stub tests | testable | sandbox credentials/contracts still required |
| Customer Verification → Amplitude | OAuth2, mTLS bundle, mapper/client, timeout/retry/error classification | testable with stub | authoritative Amplitude sandbox required |
| Payment → Amplitude | account/funds, reservation, posting, release/reversal, lookup/reconciliation | testable with deterministic provider doubles | provider endpoints/codes/certificates must be certified |
| Payment → Observed Customer | transactional outbox plus scheduled in-process consumer and projection deduplication | testable and valid for modular monolith | distributed transport not required for first release |
| Event/outbox/Kafka | canonical envelope, outbox relay, consumer idempotency, retry/DLQ foundations | testable | production Kafka topology only where selected |
| Payment → Accounting | eligibility, batch persistence, Accounting API client, idempotency and reconciliation | testable with provider stub | Accounting API sandbox/contract certification required |
| Operational Notification | intent, persistence, SMTP gateway, retry/DLQ, replay, metrics, retention | testable with SMTP provider double | production SMTP credentials/provider certification required |

## 4. Readiness levels

### 4.1 MODULAR_MONOLITH_READINESS

The gate is green only when all of the following pass:

1. reactor compilation;
2. module unit tests;
3. module architecture tests;
4. integration contract tests;
5. bootstrap intermodule tests;
6. Flyway migration validation;
7. deterministic provider failure tests;
8. no production secret committed in configuration;
9. internal transport remains in-process by default;
10. operational metrics and runbooks exist;
11. no blind retry of unknown financial outcomes;
12. replay operations remain idempotent/audited.

### 4.2 EXTERNAL_SANDBOX_CERTIFICATION

This gate requires real external inputs that cannot be manufactured by the
repository:

- TresorPay sandbox URL and credentials/certificates;
- final TresorPay JWT/API-key/JWS characteristics;
- Amplitude OAuth2 token endpoint, scopes, audience and certificate chain;
- test banking identities/accounts and deterministic balances/statuses;
- Accounting API sandbox endpoint and credentials;
- SMTP provider test credentials;
- agreed expected traces and provider references;
- authorized failure-injection scenarios.

Until those inputs exist, sandbox scenarios are `BLOCKED_EXTERNAL`, not failed.

## 5. No-go conditions

The release is not production-ready if any of the following remains true:

- an authoritative provider contract differs from the implemented provisional
  schema;
- sandbox authentication cannot be completed;
- mTLS certificate/trust chain is unverified;
- financial posting/reservation unknown-outcome lookup is unverified;
- reconciliation cannot recover after crash/restart;
- required audit trail cannot be queried;
- DLQ/replay cannot be operated safely;
- monitoring owners and alert thresholds are undefined;
- a real secret is embedded in Git-tracked configuration.

## 6. Required evidence bundle

A Phase 5 readiness review should attach:

- Maven verification result;
- E2E scenario result matrix;
- contract/sandbox certification evidence;
- Flyway validation result;
- metrics screenshots or query output;
- trace/correlation evidence;
- DLQ/replay evidence;
- reconciliation evidence;
- security configuration evidence;
- signed readiness decision.

## 7. Readiness ownership

| Evidence | Primary owner |
|---|---|
| Payment/TresorPay acceptance | Payment |
| Customer/Amplitude verification | Customer |
| Payment banking operations | Payment |
| Accounting API/reconciliation | Accounting |
| Notification provider/replay | Notification |
| Cross-cutting transport/observability | Integration/platform |
| Secrets/certificates/RBAC | Security/platform |
| Production dashboards/alerts/runbooks | Operations |
| Final go/no-go | Engineering + Operations + Security |

## 8. Exit criterion for Lot 5.8

Lot 5.8 is complete when:

- the deterministic modular-monolith gate is automated;
- all mandatory E2E scenarios are catalogued;
- external sandbox scenarios clearly identify missing inputs;
- failure and restart scenarios are represented;
- operational evidence and ownership are documented;
- the repository can generate a readiness report without pretending that an
  unavailable external sandbox has passed.
