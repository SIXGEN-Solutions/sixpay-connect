# Lot 5.6.2 — Accounting batch constitution and persistence

## Objective

Persist the canonical batches introduced in Lot 5.6.1 while preserving the
modular-monolith boundary:

```text
Payment facts
   -> PaymentAccountingCandidateSource
   -> AccountingBatchConstitutionService
   -> AccountingBatchRepository
   -> PostgreSQL
```

Accounting still has no Maven dependency on Payment.

## Constitution rules

The service:

1. resolves the AUTO or MANUAL accounting window;
2. asks the receiving-side candidate port for status-verified Payment facts;
3. removes payments already assigned to an Accounting batch;
4. applies the Lot 5.6.1 eligibility policy;
5. builds the deterministic batch idempotency key;
6. returns an existing batch when that idempotency key already exists;
7. otherwise persists the new batch atomically.

## Double-selection protection

Two database constraints protect the invariant:

- one row per `idempotency_key` in `accounting_batches`;
- one row per `payment_id` in `accounting_batch_items`.

The second constraint is intentionally global. A Payment can belong to only one
Accounting batch. An overlapping concurrent batch therefore fails even when
the two batch idempotency keys are different.

The application performs a pre-filter for already-assigned payments, but the
database uniqueness constraint is the final concurrency guard.

## Persistence ownership

Accounting owns:

- batch identity;
- batch idempotency key;
- business date;
- batch status;
- item assignment;
- item accounting status;
- TresorPay status evidence copied into the canonical Accounting item.

Accounting does not persist or reconstruct the Payment aggregate.

## Deferred

The following remain outside Lot 5.6.2:

- concrete Payment candidate-source composition adapter;
- downstream Accounting/TFJ HTTP client;
- batch submission;
- provider status polling;
- reconciliation workflow;
- TFJ format and SFTP.
