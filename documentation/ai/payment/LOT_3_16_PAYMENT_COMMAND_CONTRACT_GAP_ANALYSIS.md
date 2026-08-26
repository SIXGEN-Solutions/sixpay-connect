# LOT 3.16 — Payment Command API / TresorPay Entry

## Scope of this delivery

This delivery is contract-first. It creates the official InitiateDebit contract,
its packaged module copy, and this gap analysis. It intentionally generates no
Java implementation yet.

## Decisions applied

- Canonical field: `endToEndId`.
- `AppID` is optional.
- `nomDebiteur` and `dateExecution` are mandatory and bank-verified.
- `Idempotency-Key` and `endToEndId` are independent.
- Beneficiary amounts must sum exactly to `montantTotal`.
- Response money values use numeric `{amount, currency}` objects.
- HTTP response is 200 and the body contains `OK: "200"`.
- Initial business status is `PENDING_CONFIRMATION`.
- Callback execution is asynchronous and outside the initiation transaction.

## Security

### InitiateDebit

BOTH mutual TLS and OAuth 2.0 client-credentials JWT are required.
The access token needs `payment.initiate`.

`APIKey` and `PIN` are deliberately removed from the JSON payload. They must not
be logged, persisted, audited, or sent to the Outbox.

`LoginName` remains an interoperability field and must match the authenticated
partner identity in the JWT.

### Callback

The callback uses mutual TLS and `X-SIXPAY-Signature`, a detached JWS signature
over the exact body. Delivery is at-least-once and TresorPay deduplicates by
`eventId`.

## Confrontation with the current Payment domain

### Already compatible

- `PaymentRequestIdentity` already separates idempotency key, request fingerprint,
  and correlation ID.
- `TreasuryAllocationIntent` already enforces 1–20 allocations, unique
  beneficiaries, one currency, and exact equality between allocations and total.
- `ExternalPaymentReference` can carry `endToEndId`.
- Persistence stores status as `VARCHAR(48)` and terminal finality uses an
  explicit terminal-status list, so a new non-terminal status is structurally
  possible.

### Missing lifecycle state

`PaymentStatus` currently has no `PENDING_CONFIRMATION`.

The existing flow is:

```text
Payment.receive → RECEIVED → AUTHORIZATION_CHECKING
```

The required flow is:

```text
Payment.receive
→ RECEIVED
→ requestCustomerConfirmation
→ PENDING_CONFIRMATION
→ recordCustomerConfirmation
→ AUTHORIZATION_CHECKING
```

Preserve `Payment.receive(...)` and add a focused transition before returning the
initial API response.

### Missing domain data

`NewPaymentIntent` currently lacks:

- partner login name;
- optional application ID;
- debtor name;
- claim type;
- NUI;
- requested execution instant;
- validated callback endpoint;
- customer confirmation challenge/evidence.

Introduce focused value objects, preferably grouped in a
`PaymentInitiationContext`, rather than untyped maps.

The raw debtor RIB must not leak to logs, audit, Outbox, or state payload. It must
be transformed through the account/banking boundary into the existing protected
`DebtorAccountReference`.

### Required domain behavior

Add focused aggregate methods:

```text
requestCustomerConfirmation(...)
recordCustomerConfirmation(...)
```

Add events:

```text
PaymentCustomerConfirmationRequested
PaymentCustomerConfirmationRecorded
```

Confirmation recording must be idempotent and reject conflicting evidence.

### Persistence and migration impact

- Add non-terminal `PENDING_CONFIRMATION`.
- Extend `PaymentState` and `PaymentStateDocument`.
- Increment the JSON state schema version; do not silently change schema version 1.
- Add a new forward-only Flyway migration.
- Do not rewrite a migration already applied outside disposable development DBs.

### Contract synchronization

Also add `PENDING_CONFIRMATION` to:

```text
documentation/contracts/internal/payment-query-api-v1.yaml
backend/payment/src/main/resources/openapi/payment-query-api-v1.yaml
```

The existing exact enum-validation test will otherwise fail.

## Implementation gate

Java generation starts only after this contract and these lifecycle decisions are
accepted.

## Recommended next sequence

1. Domain lifecycle and state schema.
2. Query-contract synchronization.
3. Application command/use case.
4. Banking confirmation gateway.
5. Idempotent orchestration.
6. Golden Module API classes.
7. Outbox-driven callback dispatcher.
8. Bootstrap OpenAPI group.
9. Integration, security, replay, and callback tests.
