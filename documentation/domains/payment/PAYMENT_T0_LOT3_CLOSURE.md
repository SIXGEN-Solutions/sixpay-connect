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
