# ACCOUNTING_T1 — Programme de finalisation T1 / TFJ

## Statut

Programme actif de conception et d’implémentation incrémentale.
Le T0 Payment est fermé et n’est pas redéfini par ce programme.

## Baseline

- T0 baseline: `ce59e4dbafca0e68589f35c7df4f6d9da092352f`
- T0 known-success state: `POSTED_PENDING_TFJ`
- T0 authoritative outcome: `PaymentEventOutcome.COMPLETED`
- Physical Core Banking T1 contract: `documentation/contracts/amplitude/amplitude-accounting-entries-api-v1.yaml`
- Internal manual execution contract: `documentation/contracts/internal/accounting-t1-manual-execution-api-v1.yaml`

## Lots

1. T1.0 — Gouvernance et contrat interne Payment → Accounting + refresh AI.
2. T1.1 — TRESOR PAY status verification.
3. T1.2 — Accounting candidate projection depuis snapshots T0 finalisés.
4. T1.3 — Cutoff / eligibility / batch constitution.
5. T1.4 — Core Banking Accounting API contract.
6. T1.5 — Provider mapping / submission / recovery.
7. T1.6 — TFJ / reconciliation / Payment finality.
8. T1.7 — Closure / E2E / observability / AI documentation.

## T1.0 decisions

- Payment → Accounting crosses the module boundary through an asynchronous durable internal event contract.
- Push is used across the module boundary; pull is used only inside Accounting against its own local projection.
- Only a Payment in `POSTED_PENDING_TFJ` with authoritative T0 `COMPLETED` may produce the T1 input fact.
- A `FINALIZED` Payment financial snapshot is mandatory.
- The T0 bank reference is mandatory.
- T1 accounting/business date comes from the authoritative Core Banking accounting date fixed with the T0 context, never from a fresh Accounting wall-clock derivation.
- Consumer technical deduplication key is `eventId`; business identity is `(paymentId, financialSnapshotId)`.
- Payment owns T0 facts and snapshots. Accounting owns its local candidate projection, batches, submission and reconciliation lifecycle.
- No provider adapter, external endpoint, provider DTO or physical T1 contract is generated in T1.0.


## T1.1 decisions

- SIXPAY consults TRESOR PAY through `GET /api/v1/payments/{reference}/status` using partner OAuth2.
- The lookup verifies cross-system coherence before T1; it does not determine or rewrite T0 financial truth.
- A transaction is T1-eligible only when TRESOR PAY confirms it as `COMPLETED`.
- TRESOR PAY cannot invalidate an already-authoritative T0 `COMPLETED`.
- When TRESOR PAY is unavailable or not yet completed, the transaction is skipped for the current T1 run and remains eligible for a later verification/cutoff.
- Other verified transactions continue through T1/TFJ.
- Verification may be anticipated by a periodic worker or performed on-demand for unverified candidates; the worker cadence is deferred.
- Manual/export reconciliation processes remain external to SIXPAY until separately defined; SIXPAY may later expose extraction capability under an approved scope.


## LOT 0.5.6 — Accounting T1 Operations / Manual Execution

- The operator action launches the SIXPAY T1 treatment, not the bank-owned TFJ.
- Manual execution accepts only `businessDate` from the UI.
- The backend enforces `AccountingCutoffMode.MANUAL`.
- `financialInstitutionCode` is derived from durable Accounting candidates.
- Authorization requires `ADMIN` or `MANAGER` plus `accounting.t1.execute`.
- `AUDITOR` remains read-only.
- Existing deterministic Accounting batch idempotency remains authoritative.
- Submission/recovery reuses the approved Core Banking Accounting boundary.
- The provisional `documentation/contracts/external/accounting/accounting-batch-*`
  pack is superseded and removed.
