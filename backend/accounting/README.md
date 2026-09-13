# Accounting Module

## Purpose

Accounting owns the T+1 accounting lifecycle for financially completed Payment
operations: candidate projection, eligibility, cutoff, immutable batch
constitution, Core Banking submission tracking, TFJ confirmation and
reconciliation.

## Responsibilities

- consume approved durable Payment finalization facts into an Accounting-owned
  projection;
- verify authoritative TRESOR PAY status evidence;
- select eligible candidates for the configured business window;
- constitute immutable accounting batches;
- submit batches through `AccountingBatchGateway`;
- recover uncertain submission outcomes by lookup;
- ingest and reconcile TFJ/end-of-day confirmations;
- publish finality facts after reconciliation;
- expose read-only T1/TFJ operational visibility;
- expose approved manual T1 execution.

Provider-specific DTOs, mappings and OAuth2 configuration remain inside
Accounting. Provider-neutral HTTP/resilience belongs to Integration.

## T+1 flow

```text
Payment T0 finalized fact
-> Accounting candidate projection
-> TRESOR PAY status evidence
-> cutoff / eligibility
-> immutable Accounting batch
-> Core Banking Accounting submission
-> result or authoritative recovery
-> TFJ/end-of-day confirmation
-> reconciliation
-> finality publication
```

Accounting never reads Payment JPA entities, adapters or repositories directly.

## APIs

```text
GET  /internal/api/v1/accounting-batches
GET  /internal/api/v1/accounting-batches/{batchId}
POST /internal/api/v1/accounting-t1-executions
GET  /internal/api/v1/accounting-t1-operations
GET  /internal/api/v1/accounting-t1-operations/{candidateId}
GET  /internal/api/v1/accounting-tfj-operations
GET  /internal/api/v1/accounting-tfj-operations/{confirmationId}
```

Operational queries expose normalized Accounting-owned facts, not provider DTOs,
raw exceptions, stack traces or persistence internals.

## Persistence

Accounting owns batch, tracking, candidate, frozen-entry and TFJ confirmation
persistence. Unknown financial outcomes are reconciled before retry; blind
resubmission is prohibited.

## Structure

- domain: batches, eligibility, tracking and operational models;
- application: selection, constitution, submission, recovery and queries;
- api: HTTP controllers, validation and mapping;
- infrastructure: persistence and provider adapters.

## Validation

```bash
mvn -pl accounting -am test
mvn -pl accounting -am clean verify
mvn -pl accounting -am -Pfull-tests clean verify
```
