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
