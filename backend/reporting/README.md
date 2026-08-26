# Reporting Module

## Purpose

The Reporting module owns immutable Payment audit queries and controlled audit
exports. It is a read-oriented module and does not own Payment state
transitions.

## APIs

    GET /internal/api/v1/payments/{paymentId}/timeline
    GET /internal/api/v1/payment-audit-records
    GET /internal/api/v1/payment-audit-records/{auditId}
    POST /internal/api/v1/payment-audit-exports
    GET /internal/api/v1/payment-audit-exports/{exportId}

Read operations require payment audit read authority. Export operations also
require payment audit export authority.

## Responsibilities

- query masked Payment audit records and timelines;
- apply authenticated cursors and access checks;
- record audit-access activity;
- create idempotent export jobs;
- generate and store controlled export artifacts.

## Boundaries

- Payment remains the owner of Payment state and financial transitions.
- Reporting never initiates posting, reversal or other financial commands.
- Operational incident querying belongs to Administration.
- Persistence adapters and export stores remain inside Reporting infrastructure.

## Validation

From backend:

    mvn -pl reporting -am test
    mvn -pl reporting -am clean verify
    mvn -pl reporting -am -Pfull-tests clean verify

The full-tests command requires Docker when PostgreSQL integration tests are
selected.

## Persistence ownership

Reporting owns these production tables:

| Table | Purpose |
|---|---|
| reporting_payment_audit_evidence | Payment audit read evidence |
| reporting_payment_audit_export_job | Controlled audit export jobs |

Reporting does not update Payment-owned tables or persist financial state.

Schema:
backend/reporting/src/main/resources/db/migration/V500__reporting_baseline.sql
