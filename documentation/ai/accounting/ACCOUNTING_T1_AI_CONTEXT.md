# ACCOUNTING_T1 — Active AI context

Status: ACTIVE PROGRAM CONTEXT
Authority: derived from current architecture, approved Payment T0 implementation and T1.0 decisions.
It does not replace architecture, requirements or physical contracts.

## ACTIVE

- Atomic Payment T0 is closed.
- Payment owns finalized T0 financial snapshots.
- Payment→Accounting boundary is an asynchronous durable internal event.
- Push is used across the module boundary. Pull is local to Accounting's own projection.
- Only `POSTED_PENDING_TFJ` + authoritative `PaymentEventOutcome.COMPLETED` may enter T1.
- `PaymentFinancialEventSnapshot.FINALIZED` is mandatory.
- T0 bank reference is mandatory.
- T1 business/accounting date is the authoritative Core Banking accounting date fixed with T0.
- Consumer deduplication uses `eventId`; business identity is `(paymentId, financialSnapshotId)`.
- Accounting must not recreate financial entries from mutable Payment state.
- Payment owns T0 facts. Accounting owns its local candidate projection and T1 batch/submission/reconciliation lifecycle.
- Payment owns final states such as `TREASURY_INTEGRATED` and `REVERSAL_REQUIRED`.

## TO_DEFINE / DEFERRED

- TRESOR PAY T1.1 status lookup is `GET /api/v1/payments/{reference}/status` using partner OAuth2.
- TRESOR PAY status verification is an additional T1 eligibility/coherence fact; it never invalidates authoritative T0 `COMPLETED`.
- Only TRESOR PAY `COMPLETED` evidence is accepted as paid for T1.1.
- If TRESOR PAY is unavailable or not yet completed, exclude the transaction from the current T1 selection and retry verification for a later cutoff while continuing other verified transactions.
- Verification may be performed by a periodic pre-cutoff worker or on-demand for unverified candidates; scheduler cadence remains TO_DEFINE.
- Accounting candidate persistence schema for T1.2.
- Physical Core Banking Accounting API for T1.4.
- Provider DTOs/mappings for T1.5.
- TFJ transport and final reconciliation rules for T1.6.

## FORBIDDEN

- Direct Accounting access to Payment JPA entities, infrastructure or repositories.
- Provider adapter generation in T1.0.
- Treating current `accountingapi` paths/DTOs as contract authority.
- Persisting full Amplitude bkmvti entities in SIXPAY.
- Reconstructing T1 financial lines from current Payment state when frozen T0 snapshots exist.
- Blind replay after unknown provider submission outcome.
- Reintroducing the removed split-leg T0 posting model or `DEBIT_CONFIRMED` as an active T0 state.
