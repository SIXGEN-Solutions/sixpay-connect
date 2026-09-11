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
