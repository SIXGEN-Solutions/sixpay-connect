# Payment T0 — LOT 3 closure

## Scope

LOT 3 closes the runtime T0 application path after customer confirmation and
SIXPAY authorization, including durable uncertain-outcome recovery.

The closure does not modify `POST /v1/payments/initiate`, does not introduce a
standalone Core Banking funds-check endpoint, does not add a second financial
execution call, and does not implement TFJ/T+1 reconciliation.

## Runtime path

The active runtime path is:

1. OTP verification completes.
2. SIXPAY authorization approves the Payment.
3. `PaymentFundsControlRequested` is persisted and relayed.
4. funds-control preparation advances the Payment.
5. Treasury account resolution is performed through the Payment-owned port.
6. `PaymentApprovedForPosting` is persisted and relayed.
7. the T0 identity is created once or the persisted identity is reused.
8. the immutable financial snapshot is finalized or reused.
9. Core Banking execution context is resolved.
10. the sole atomic Payment event POST is executed.
11. the Payment outcome is persisted as completed, rejected or unknown.

## Durable recovery

A Payment persisted in `POSTING_OUTCOME_UNKNOWN` is recovered through
`RecoverUnknownPaymentT0UseCase`.

Recovery:

1. reloads the durable Payment;
2. requires `POSTING_OUTCOME_UNKNOWN`;
3. reuses the persisted posting instruction/idempotency key;
4. performs authoritative lookup by payment reference;
5. if necessary, performs lookup by idempotency key;
6. resolves and persists the aggregate outcome.

Recovery never invokes a new financial POST.

## Partner GET consultation after T0 (R5)

The existing read-only Partner endpoint is
`GET /api/v1/integrations/payments/{paymentReference}` (contract
`documentation/contracts/external/payment-consultation-api-v1.yaml`). It returns
only the current **persisted SIXPAY Payment** view. A delayed or missing
asynchronous callback does not imply a failed or completed bank operation:
the authorized initiating M2M machine may reconsult the Payment.

- `POSTING_PENDING`: T0 posting is still pending in SIXPAY.
- `POSTING_OUTCOME_UNKNOWN`: authoritative T0 outcome is not yet established;
  this GET does **not** query the bank and must not be interpreted as rejection.
- `POSTED_PENDING_TFJ`: authoritative T0 success has already been persisted;
  this is the Payment status, not a raw bank `COMPLETED` field.
- `REJECTED`: the Payment has been rejected after an authoritative decision;
  this is not a raw bank `REJECTED` field.
- Other existing lifecycle statuses remain valid. No new status or field is
  introduced by R5.

The response shape remains unchanged: `paymentId`, `paymentReference`,
`externalPaymentReference`, `status`, `amount` (`amount`, `currency`),
`receivedAt`, `updatedAt`, and optional/nullable `finalizedAt`.
No bank payload, internal evidence, OTP, full account or retry metadata is
exposed. The GET neither changes Payment nor triggers Core Banking, OTP,
financial execution, internal R2 recovery or manual R3 reconciliation.

The request requires a valid M2M caller, `payment.read` scope, the existing
object-level partner and initiating-machine binding, and `X-Correlation-ID`.
Missing or invisible Payments return the same `404`; missing/invalid JWT
returns `401`, and inadequate scope returns `403`. mTLS is enforced at the
ingress/gateway boundary as specified by the existing contract.

**Contract governance remains unchanged:** `ACTIVE_MVP`,
`PENDING_APPROVAL`, `REFERENCE_ONLY`, `codeGenerationAllowed: false`.
This documentation-only alignment does not approve code generation or change
any request/response schema, permission or operational trigger.

## Single logical T0 execution guard

The T0 execution path is at-most-once at SIXPAY orchestration level:

- the finalized financial snapshot must match the persisted instruction fingerprint before any Core Banking context allocation;
- `POSTING_PENDING` with the same persisted instruction returns idempotently without resolving a new banking context and without issuing another POST;
- a stale concurrent caller that loses the durable posting-authorization mutation returns without issuing a POST;
- the same idempotency key with a different financial request is a conflict before any banking call.

## Validation gates

The final Payment gate is:

```bash
bash scripts/validation/validate-payment-phase3.sh
```

It executes:

- focused T0 orchestration/recovery/architecture tests;
- Payment module unit tests;
- the Payment `full-tests` verification gate;
- explicit financial snapshot concurrency validation.

A LOT 3 closure may be declared `REPOSITORY_VALIDATED` only when the required
commands have actually completed with exit code 0 on the selected revision.

## Out of scope

- TFJ/T+1 reconciliation changes;
- automatic reversal;
- public API generation from unapproved/reference-only contracts;
- new persistence schema or migration;
- new Core Banking financial endpoint;
- blind replay after unknown financial outcome.


## LOT 3.5 state coherence closure

The active T0 runtime uses only `PaymentEventOutcomeSnapshot`. The former
split-leg posting model, its interpretation policy, gateways, status
reconciliation path and dedicated Amplitude adapters are removed from active
code.

```text
POSTING_PENDING + COMPLETED -> POSTED_PENDING_TFJ
POSTING_PENDING + REJECTED  -> REJECTED
POSTING_PENDING + UNKNOWN   -> POSTING_OUTCOME_UNKNOWN
POSTING_OUTCOME_UNKNOWN + authoritative COMPLETED -> POSTED_PENDING_TFJ
POSTING_OUTCOME_UNKNOWN + authoritative REJECTED  -> REJECTED
POSTING_OUTCOME_UNKNOWN + still UNKNOWN            -> unchanged
```

`DEBIT_CONFIRMED` is vocabulary-only for the atomic MVP. `REVERSAL_REQUIRED`
remains valid through an explicitly governed reversal decision such as TFJ
reconciliation, not as a partial T0 outcome.

Payment JSON state persistence is schema v7. New writes do not persist
`postingOutcomeEvidence`. Older JSON may contain the removed field, but a
legacy-only financial state is never silently translated into atomic evidence.
