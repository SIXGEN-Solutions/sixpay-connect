# Accounting Module

## Purpose

The Accounting module owns accounting-batch constitution, submission tracking
and reconciliation for completed Payment operations.

## Responsibilities

- select eligible Payment records for accounting;
- build and persist accounting batches and batch items;
- submit batches through the provider-specific accounting adapter;
- reconcile acknowledged, rejected and unknown outcomes;
- expose the internal accounting-batch query API.

Provider-specific DTOs, mappings and OAuth2 client configuration remain inside
Accounting. Provider-neutral HTTP and resilience support belongs to
backend/integration.

## MVP end-of-day flow

The Accounting module is the owner of the T+1 accounting lifecycle after a
Payment has already completed its T0 financial execution.

The target MVP flow is:

1. select successful, unbatched Payment candidates for the applicable cut-off;
2. obtain/use authoritative TRESOR PAY status evidence for each candidate;
3. retain accounting-eligible candidates;
4. constitute and persist an Accounting batch;
5. submit the payment batch through `AccountingBatchGateway`;
6. use the Core Banking Accounting API in the MVP;
7. let Core Banking generate and post its own accounting entries;
8. reconcile acknowledged, rejected and unknown outcomes.

SIXPAY does not generate Core Banking journal lines for the MVP.

CSV/file submission is a deferred transport option. It requires a separate
approved file-layout, integrity, transport, acknowledgement and reconciliation
contract before implementation.


## API

Base path: /internal/api/v1/accounting-batches

| Method | Endpoint | Purpose |
|---|---|---|
| GET | /internal/api/v1/accounting-batches | Search accounting batches |
| GET | /internal/api/v1/accounting-batches/{batchId} | Retrieve one batch |

The active contract is:
documentation/contracts/internal/accounting-query-api-v1.yaml

## Persistence

Accounting owns its PostgreSQL tables and Flyway migrations. Repository
adapters and JPA entities remain under the infrastructure.persistence package.
Unknown financial outcomes are reconciled before any retry; blind resubmission
is prohibited.

## Structure

The module follows the Partner reference layering:

- domain: batches, tracking and accounting policies;
- application: constitution, selection and reconciliation use cases;
- api: HTTP controllers, validation and mapping;
- infrastructure: persistence and accounting-provider adapters.

## Validation

From backend:

    mvn -pl accounting -am test
    mvn -pl accounting -am clean verify
    mvn -pl accounting -am -Pfull-tests clean verify

The full-tests command requires Docker for PostgreSQL integration tests.

## Persistence ownership

Accounting owns these production tables:

| Table | Purpose |
|---|---|
| accounting_batches | Accounting batch identity and submission state |
| accounting_batch_items | Payment items assigned to a batch |
| accounting_batch_tracking | Batch reconciliation tracking |
| accounting_batch_item_tracking | Item-level reconciliation tracking |

Schema:
backend/accounting/src/main/resources/db/migration/V400__accounting_baseline.sql

## ACCOUNTING_T1 boundary

T1 consumes an Accounting-owned local projection populated from an approved durable internal Payment event. Accounting must not access Payment JPA entities, infrastructure adapters or repositories directly.

Only Payments whose T0 is authoritatively `COMPLETED`, whose Payment status is `POSTED_PENDING_TFJ`, and whose Payment-owned financial snapshot is `FINALIZED` may produce the T1 input fact. The physical Core Banking Accounting API remains `TO_DEFINE`; existing `accountingapi` classes are not contract authority until T1.4.

T1.1 adds the Accounting-owned provider-neutral boundary for TRESOR PAY payment-status verification. The supplied external operation is `GET /api/v1/payments/{reference}/status`; only `COMPLETED` evidence qualifies a candidate for T1. No provider adapter is introduced in T1.1.

### T1.2 candidate projection

Accounting owns a durable local candidate projection populated from the approved
Payment T0-finalized semantic fact. `PaymentAccountingCandidateSource` reads only
this local projection. Replay is deduplicated by `eventId` and business identity
`(paymentId, financialSnapshotId)`.

### T1.3 cutoff and snapshot-backed batch constitution

T1.3 reuses the existing cutoff/eligibility/builder/service flow. Newly constituted batch
items are immutable copies of the T1.2 financial snapshot identity and frozen entries.
Batch idempotency is derived from sorted `(paymentId, financialSnapshotId)` business
identities, and selected local candidates are assigned to the persisted batch transactionally.
Pre-T1.3 historical rows remain readable without an invented snapshot backfill. No physical
Core Banking T1 mapping is authorized by this lot.

### T1.3 cutoff and immutable batch snapshots

Active batch constitution reads the Accounting-owned T1.2 projection,
applies cutoff/TRESOR PAY/unassigned eligibility, and persists immutable
financial snapshot identity plus ordered frozen entries in each new batch item.
Idempotency uses `(paymentId, financialSnapshotId)` and candidate assignment
to `batchId` occurs in the same transaction.

### LOT 5.6.1 — T1 operational ownership model

Accounting owns the normalized operational interpretation of T1 facts used for
future operator visibility. Provider evidence remains source evidence, but the
operator model must not expose provider DTOs, transport exceptions, stack traces
or persistence internals directly.

The approved O01-O07 decisions are represented by domain types under
`com.sixpay.accounting.domain.model`:

- `AccountingT1OperationalCandidateStatus` models the candidate lifecycle visible
  to an operator before and after batch assignment;
- `AccountingT1EligibilityReason` normalizes Accounting-owned ineligibility
  reasons without exposing raw provider failure details;
- `AccountingT1TechnicalIssue` exposes only bounded technical categories;
- `AccountingT1OperationalSnapshot` combines Accounting-owned candidate facts,
  TRESOR PAY status evidence, and the resolved Accounting selection window.

No constitution-attempt model is introduced because no durable model exists for
that notion. Recovery remains exposed only where Accounting already owns a durable
state, notably `AccountingBatchTracking` and its submission/reconciliation state.

LOT 5.6.1 does not add or modify an HTTP contract, endpoint, permission, database
schema, migration or Angular model. Any operator API remains a subsequent,
separately approved contract step.

### LOT 5.6.2 — T1 operational query contract

The internal read-only contract
`documentation/contracts/internal/accounting-t1-operational-query-api-v1.yaml`
defines operator visibility over the Accounting-owned T1 operational model.

The contract exposes search and detail queries only. It does not execute T1,
invoke TRESOR PAY, constitute a batch, submit Accounting entries or trigger
recovery. ADMIN, MANAGER and AUDITOR are read-only consumers through
`accounting.read`.

The contract mirrors the LOT 5.6.1 normalized model: candidate status, persisted
TRESOR PAY status evidence, Accounting-owned eligibility reason, resolved
selection window, bounded technical issue category and optional batch assignment.

No constitution-attempt field is introduced because no durable Accounting-owned
model currently exists for that notion. Provider DTOs, raw exceptions, transport
failures, stack traces and persistence internals remain outside the API surface.

### LOT 5.6.3 — T1 operational query security

Accounting read-only operational visibility follows the approved internal
security profile of the LOT 5.6.2 contract.

Access requires both:

- one of the operator roles `ADMIN`, `MANAGER` or `AUDITOR`;
- the `accounting.read` OAuth2/JWT scope, mapped at runtime to
  `SCOPE_accounting.read`.

The role check alone is insufficient, and the scope alone is insufficient.
`AUDITOR` remains strictly read-only. No T1 execution permission is introduced
by this lot; manual execution continues to require `accounting.t1.execute`.

The existing Accounting batch query controller is aligned to the same read
security rule so the Accounting read surface does not have two different
authorization models.

### LOT 5.6.4 — T1 operational backend query

The backend now exposes the approved read-only T1 operational query through the
Accounting application boundary.

The implementation adds a dedicated input use case, output query port,
persistence adapter reuse over the existing Accounting candidate projection,
HTTP controller and response DTOs aligned with the LOT 5.6.2 contract.

No new database table or migration is introduced. Query execution is read-only,
remains Accounting-owned, and does not trigger TRESOR PAY verification, batch
constitution, submission, reconciliation or recovery actions.

Security remains the LOT 5.6.3 rule: `ADMIN`, `MANAGER` or `AUDITOR` plus
`SCOPE_accounting.read`.

### LOT 5.6.5 — T1 operational query tests

The operational query test suite covers the approved read-only behavior across
the HTTP and application layers.

Coverage includes:

- search filters and response pagination;
- detail lookup and not-found mapping;
- ADMIN/MANAGER/AUDITOR plus `SCOPE_accounting.read`;
- rejection when the role or scope is missing;
- unauthenticated access;
- normalized operational states for missing, completed and non-completed
  TRESOR PAY evidence;
- normalized ineligibility reasons;
- payment-reference normalization;
- pagination guards.

The tests do not invoke provider verification, batch constitution, submission,
reconciliation or recovery. No database schema or contract change is introduced
by this sub-lot.

### LOT 5.6.7 — Operational observability closure

LOT 5.6 closure fixes align the operational query with the Accounting-owned
runtime model:

- selection windows are resolved through the configured `AccountingCutoffPolicy`
  instead of a synthetic UTC calendar day;
- status-filtered queries compute pagination after normalized operational-state
  derivation so `content`, `totalElements` and `totalPages` are coherent;
- durable batch recovery state is exposed only through normalized
  `AccountingT1TechnicalIssue` values:
  `OUTCOME_UNKNOWN` maps to `ACCOUNTING_SUBMISSION_OUTCOME_UNKNOWN` and
  `RECONCILIATION_REQUIRED` maps to `ACCOUNTING_RECONCILIATION_PENDING`;
- no raw provider error or persistence detail is exposed;
- `TRESORPAY_STATUS_LOOKUP_UNAVAILABLE` is not synthesized when no durable
  Accounting-owned failure fact exists; an unverified candidate remains
  `AWAITING_TRESORPAY_VERIFICATION` with `TRESORPAY_STATUS_UNAVAILABLE`.

No contract, database schema, migration, provider integration or security rule
is changed by this closure sub-lot.

### LOT 5.7.1 — TFJ operational ownership and operator model

Accounting owns the internal operator representation of persisted TFJ /
reconciliation facts.

The operator model is read-only. `TfjOperationalSnapshot` is derived from
`TfjConfirmation` and exposes normalized Accounting-owned facts only. It does
not expose provider transport payloads, persistence entities, raw exceptions or
new operational commands.

Operator categories are intentionally limited to facts already represented by
the durable TFJ model:

- `MATCHED`;
- `QUARANTINED_UNMATCHED`;
- `QUARANTINED_AMBIGUOUS`;
- `FAILED`;
- `FINALITY_PUBLICATION_PENDING`;
- `COMPLETED`.

No endpoint, database schema, migration, retry, replay, force-match, resolve,
reverse or mark-integrated command is introduced by LOT 5.7.1.
