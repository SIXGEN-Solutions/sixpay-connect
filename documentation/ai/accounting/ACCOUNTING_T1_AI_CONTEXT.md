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

## T1.2 active implementation context

T1.2 persists an Accounting-owned candidate projection from the approved durable
Payment -> Accounting semantic fact.

- publication preconditions: `POSTED_PENDING_TFJ`, T0 `COMPLETED`, bank reference,
  finalized financial snapshot and authoritative Core Banking accounting date;
- frozen snapshot facts and ordered entries are copied into Accounting;
- technical replay identity: `eventId`;
- business identity: `(paymentId, financialSnapshotId)`;
- TRESOR PAY `COMPLETED`, cutoff membership and `batchId == null` are selection-time criteria;
- no Accounting access to Payment JPA/repositories/infrastructure;
- physical Core Banking T1 API is defined by the approved T1.4 contract.

## T1.3 active implementation context

T1.3 freezes the candidate snapshot into the Accounting batch. Newly constituted
`AccountingBatchItem` instances carry the finalized snapshot identity, debtor and creditor
references and the two immutable DEBIT/CREDIT entries. Batch idempotency uses sorted
`paymentId:financialSnapshotId` identities. Candidate `batchId` assignment occurs
transactionally after durable batch persistence. Historical V400 rows are preserved without
invented backfill. T1.4 defines the physical Core Banking Accounting contract.

## T1.4 active implementation context

T1.4 defines the physical Core Banking Accounting contract without generating the
provider adapter owned by T1.5.

- contract: `documentation/contracts/amplitude/amplitude-accounting-entries-api-v1.yaml`;
- submit operation: `POST /api/v1/accounting-entries`;
- authoritative recovery:
  `GET /api/v1/accounting-entries/batches/{batchId}` and
  `GET /api/v1/accounting-entries/idempotency/{idempotencyKey}`;
- the existing Core Banking security profile is reused: OAuth2 Client Credentials
  plus mTLS, `X-Correlation-ID`, `X-Financial-Institution-Code` and
  `Idempotency-Key`;
- one contract supports immediate/conclusive (`200`) and accepted/asynchronous
  (`202`) behavior;
- canonical Accounting batches are mapped to a reduced provider payload; full
  historical `bkmvti` persistence or schema reproduction in SIXPAY is forbidden;
- provider account mapping uses `age-ncp-clc`;
- canonical `DEBIT` maps to provider `D`; canonical `CREDIT` maps to provider `C`;
- each newly constituted T1.3 batch item submits the two immutable frozen T0
  accounting entries, identified by `paymentReference`, `financialSnapshotId`
  and T0 `bankReference`;
- provider batch statuses are `ACCEPTED`, `PROCESSING`, `COMPLETED`;
- provider item results are independently `SUCCESS`, `FAILED`, `UNKNOWN`;
- a `COMPLETED` batch does not imply that all items succeeded;
- failed or unknown items do not block successful items and never invalidate T0;
- unknown provider outcomes require authoritative lookup before retry; blind
  financial replay is forbidden;
- stable batch idempotency remains rooted in the T1.3 sorted
  `paymentId:financialSnapshotId` identities;
- provider DTOs, mapping and HTTP client implementation remain T1.5;
- detailed regularization of FAILED/UNKNOWN items and final TFJ reconciliation
  remain T1.6 / La Regionale and TRESOR PAY policy.
