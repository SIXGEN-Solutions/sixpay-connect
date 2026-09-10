# ACCOUNTING_T1 — T1.1 TRESOR PAY status verification decisions

## Purpose

Define the authoritative TRESOR PAY status evidence required before a T0-completed
Payment may be selected for T1 accounting.

## Business rationale

A Payment may be financially completed at T0 in SIXPAY/Core Banking while TRESOR PAY
still exposes an unpaid or non-final state. T1 must not include such a transaction
until TRESOR PAY confirms the corresponding payment as paid/completed.

This verification serves both cross-system business coherence and accounting eligibility.
It never invalidates or rolls back an authoritative T0 `COMPLETED` result.

## Source-of-truth order

1. Core Banking / Payment T0 remains authoritative for financial execution.
2. TRESOR PAY status is an additional downstream coherence requirement for T1 eligibility.
3. A TRESOR PAY non-paid or unavailable status does not rewrite T0.

## External API supplied by TRESOR PAY

Direction: SIXPAY -> TRESOR PAY

`GET /api/v1/payments/{reference}/status`

Authentication profile supplied for T1.1: OAuth2 partner access.

Supplied response fields:
- `reference`
- `transaction_id`
- `status`
- `payment_method`
- `operator_reference`
- `debit_effectue`
- `quittance_disponible`
- `updated_at`
- `failure_reason`

Exact OAuth2 scopes, endpoint base URL, error codes, timeout semantics and retry
parameters remain to be approved before provider adapter generation.

## Accounting eligibility

A transaction is T1-eligible only when all prior T1.0 conditions are satisfied and
TRESOR PAY evidence confirms the payment as paid/completed.

For T1.1, `COMPLETED` is the accepted provider status.

Returned `reference` must equal the requested payment reference.

## Verification timing

T1.1 does not force a single scheduler design.

Approved functional behavior:
- SIXPAY may verify status before cutoff using a periodic worker; or
- SIXPAY may verify only unverified candidates while preparing T1.

In both cases, only candidates with stored, verified `COMPLETED` evidence are selected.

Worker cadence belongs to T1.2/T1.3 operational design.

## TRESOR PAY unavailable / non-final

If TRESOR PAY is unavailable or the payment is not yet confirmed paid:
- do not fail the already-completed T0 Payment;
- do not include the transaction in the current T1 batch;
- leave it eligible for a future verification/cutoff;
- continue T1/TFJ processing for other verified transactions.

Operational/manual handling of unmatched transactions may include SIXPAY extraction
of reconciliation data. External file exchange/validation is owned by La Régionale /
TRESOR PAY and remains outside SIXPAY until separately defined.

## Recovery and idempotence

GET status verification is read-only. Repeating the same lookup creates no financial
side effect. SIXPAY stores normalized observation evidence and may refresh it later.

## T1.1 exclusions

T1.1 does not define:
- Core Banking Accounting T1 API;
- candidate persistence;
- worker scheduling cadence;
- CSV/manual reconciliation transport;
- any change to T0 financial truth.
