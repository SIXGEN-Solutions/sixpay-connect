# Phase 5 E2E acceptance scenarios

## Legend

- `MODULAR_MONOLITH`: deterministic local/CI scenario.
- `EXTERNAL_SANDBOX`: requires a real external provider.
- `PASS`: expected automated evidence exists.
- `BLOCKED_EXTERNAL`: external characteristics/credentials are missing.

## Mandatory scenarios

| ID | Gate | Scenario | Expected result |
|---|---|---|---|
| E2E-01 | MODULAR_MONOLITH | TresorPay-style valid Payment request enters SIXPAY with correlation and idempotency | request accepted exactly once; duplicate key is safe |
| E2E-02 | MODULAR_MONOLITH | Customer Verification returns positive banking evidence | Payment continues with verified evidence |
| E2E-03 | MODULAR_MONOLITH | Customer Verification technical timeout | bounded retry/classification; no false business rejection |
| E2E-04 | MODULAR_MONOLITH | Payment reservation/posting succeeds | bank references persisted; no duplicate financial side effect |
| E2E-05 | MODULAR_MONOLITH | Posting/reservation transport outcome is unknown | no blind retry; lookup/reconciliation path required |
| E2E-06 | MODULAR_MONOLITH | Payment fact reaches Observed Customer projection | outbox survives restart; projection is idempotent |
| E2E-07 | MODULAR_MONOLITH | Posted Payment becomes Accounting candidate/batch | eligible item assigned once; batch idempotence preserved |
| E2E-08 | MODULAR_MONOLITH | Accounting submission outcome is unknown | lookup by idempotency/reference before any resubmission |
| E2E-09 | MODULAR_MONOLITH | Operational notification provider fails transiently | Payment/Accounting state unchanged; retry then DLQ if exhausted |
| E2E-10 | MODULAR_MONOLITH | DEAD_LETTERED notification is replayed | same notification identity; replay audit present; fresh bounded retry cycle |
| E2E-11 | MODULAR_MONOLITH | duplicate asynchronous delivery/restart | functional effect occurs once |
| E2E-12 | MODULAR_MONOLITH | terminal data retention executes | only terminal old records are purged |
| E2E-13 | EXTERNAL_SANDBOX | TresorPay real security and callback exchange | provider-certified request/callback/security evidence |
| E2E-14 | EXTERNAL_SANDBOX | Amplitude customer verification | real OAuth2+mTLS, mappings and error codes certified |
| E2E-15 | EXTERNAL_SANDBOX | Amplitude Payment reservation/posting/lookup/reversal | real financial-operation semantics certified |
| E2E-16 | EXTERNAL_SANDBOX | Accounting API submit/lookup/reconciliation | real batch schema and provider statuses certified |
| E2E-17 | EXTERNAL_SANDBOX | SMTP provider acceptance | credential/TLS/provider acceptance certified |

## Failure-injection matrix

| Failure | Expected SIXPAY behavior |
|---|---|
| HTTP connect/read timeout on query | bounded retry if operation is safe |
| timeout after financial command emission | outcome unknown; reconcile/lookup before action |
| HTTP 401/403 | authentication/configuration failure; no blind retry |
| HTTP 429/5xx | retry only according to operation semantics |
| malformed provider response | protocol/invalid-response classification |
| database restart after outbox commit | message remains recoverable |
| consumer restart after processing before acknowledgement | deduplication prevents functional duplicate |
| SMTP unavailable | Notification retries independently from business transaction |
| Accounting provider unavailable | batch remains visible/reconcilable |
| Kafka unavailable where Kafka transport is selected | outbox remains durable; no business-event loss |

## Trace evidence

Every scenario that crosses an integration boundary should demonstrate:

- correlation ID propagation;
- request ID where synchronous HTTP applies;
- stable event ID where asynchronous delivery applies;
- causation ID for derived distributed events;
- no secret/raw sensitive payload in operational logs;
- provider reference captured only when contractually allowed.

## Acceptance rule

A missing real provider sandbox does not make the deterministic modular-monolith
gate fail. It makes only the corresponding `EXTERNAL_SANDBOX` scenario
`BLOCKED_EXTERNAL`.

Production go-live requires all externally required scenarios to move from
`BLOCKED_EXTERNAL` to `PASS`.
