# Lot 5.6.1 — Payment → Accounting eligibility and batch model

## Boundary

Accounting does not depend on Payment infrastructure and never receives the
Payment aggregate or Payment JPA entities/repositories.

Payment owns immutable T0 financial-event and financial-entry snapshot facts.
An approved internal boundary/composition adapter projects the subset required
by Accounting into Accounting-owned candidate/item models.

Accounting therefore consumes frozen historical execution facts; it does not
rebuild accounting lines from current Payment, Partner or provider configuration.

## Eligibility baseline

A candidate is eligible when:

1. its Payment occurrence belongs to the selected accounting window;
2. a TresorPay payment-status lookup evidence is present;
3. the status lookup was completed no later than the cut-off.

Lot 5.6.1 deliberately does not invent a provider-status allow-list. The
provider status is preserved as an opaque value. Once TresorPay defines which
statuses are accounting-eligible, only the eligibility policy changes.

## Cut-off

Temporary default proposal:

- timezone: `Africa/Douala`;
- daily cut-off: `23:00`;
- AUTO: use the latest closed business window;
- MANUAL: operator supplies the business date.

A business date D represents `[D-1 23:00, D 23:00)` in the configured timezone.

## Idempotence

The batch idempotency key is the SHA-256 of:

`financialInstitutionCode | businessDate | sorted(paymentIds)`

The same candidate set therefore produces the same key regardless of iteration
order. The batch UUID remains a separate SIXPAY tracking identifier.

## Provisional statuses

Batch:
- `COMPLETED`;
- `NOT_COMPLETED`.

Item:
- `PENDING`;
- `COMPLETED`;
- `REJECTED`;
- `RECONCILIATION_REQUIRED`.

## Financial-entry source

The debit/credit line instructions used by T1 originate from immutable T0
financial-entry snapshots produced and frozen by Payment.

Accounting may enrich them only with Accounting-owned batch metadata and
eligibility/reconciliation evidence. It must not mutate the original financial
meaning of the frozen lines.

Core Banking validates and effectively posts/accounts the submitted lines. The
physical Accounting API endpoint and final provider batch/line schema remain
`TO_DEFINE`.

## Explicitly outside the current formalisation

- physical Accounting API endpoint;
- exact final provider field/code subset for the T1 batch;
- TFJ control totals;
- file naming;
- SFTP host/key/directories;
- technical SFTP acknowledgement.

These require the dedicated T1 implementation/contract lot.
