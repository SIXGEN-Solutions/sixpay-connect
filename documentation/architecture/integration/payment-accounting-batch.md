# Lot 5.6.1 — Payment → Accounting eligibility and batch model

## Boundary

Accounting does not depend on the Payment module and never receives the Payment
aggregate. Payment facts are converted by a future composition adapter into the
Accounting-owned `AccountingPaymentCandidate`.

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

## Explicitly outside Lot 5.6.1 / SIXPAY accounting-domain model

- TFJ physical format;
- accounting codes;
- debit/credit rules;
- TFJ control totals;
- file naming;
- SFTP host/key/directories;
- technical SFTP acknowledgement.

Those remain responsibilities of the downstream Accounting/TFJ provider.
