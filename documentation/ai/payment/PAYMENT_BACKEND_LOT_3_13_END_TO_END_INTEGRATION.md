# SIXPAY CONNECT — Phase 3 / Lot 3.13

## End-to-End Integration

### Requested chain

```text
TresorPay
    ↓
Payment
    ↓
Amplitude
    ↓
Accounting
    ↓
Notification
```

## Repository findings

A production-grade full-system E2E cannot yet be claimed on the authoritative
branch.

### TresorPay

The Payment module has a domain reception service, but the published REST
contract currently exposed by the module is the internal read-only Payment
Query API. No approved TresorPay payment-command controller is available for
an HTTP-driven E2E.

### Payment

Payment persistence, audit, outbox, idempotency and application orchestration
are present and can be tested with PostgreSQL.

### Amplitude

Gateway ports and conditional adapters exist. The concrete HTTP
`AmplitudeBankingClient` implementation remains absent, so the test uses a
contract double behind the real `AmplitudePostingAdapter`.

### Accounting

The Accounting Maven module currently exposes only its module foundation. No
verified Payment-event consumer or posting API is available for this E2E.

### Notification

The Notification module has Kafka, persistence and email dependencies, but no
Payment consumer was verified from the authoritative implementation during
this lot.

## Executable scope

`PaymentEndToEndIntegrationIT` validates:

1. a TresorPay-originated Payment record exists in PostgreSQL;
2. the Payment outbox stores the inbound Payment event;
3. the real Amplitude posting adapter delegates to its client contract;
4. subsequent integration events remain durable and ordered;
5. Accounting receives the posting-completed envelope;
6. Notification receives only the final-result envelope;
7. Payment identity and correlation are preserved end to end;
8. Payment production code has no direct dependency on Accounting or
   Notification.

## Contract doubles

The following remain test probes:

```text
TresorPay entry
AmplitudeBankingClient
Accounting consumer
Notification consumer
```

They are explicit and local to the integration test. No production stub or
fake success behavior is introduced.

## What this test does not prove

It does not prove:

- TresorPay HTTP contract compatibility;
- OAuth2 or mTLS interoperability;
- actual Amplitude behavior;
- Kafka broker publication;
- Accounting business posting;
- email/SMS delivery;
- TFJ file ingestion;
- distributed rollback.

## Full E2E exit criteria

A later full-system E2E requires:

- approved TresorPay payment-command API;
- concrete secured Amplitude HTTP client;
- outbox Kafka relay;
- Accounting consumer and persistence;
- Notification Payment consumer;
- Kafka Testcontainer or approved broker fixture;
- WireMock or bank sandbox;
- cross-module bootstrap test application;
- trace and correlation assertions across all consumers.

Until those items exist, this lot is correctly classified as:

```text
EXECUTABLE_MODULE_CHAIN_WITH_CONTRACT_DOUBLES
```
