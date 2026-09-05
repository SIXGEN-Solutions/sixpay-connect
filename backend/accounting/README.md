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
