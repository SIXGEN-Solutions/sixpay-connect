# Phase 6 — Lot 6.5 Controlled Audit Export

## Contract operations

- `POST /internal/api/v1/payment-audit-exports`
- `GET /internal/api/v1/payment-audit-exports/{exportId}`

Both require `payment.audit.read` and `payment.audit.export`.

## Workflow

```text
POST
  ↓
validate scopes + Idempotency-Key + bounded period + businessPurpose
  ↓
persist ACCEPTED job (durable)
  ↓
202 Accepted + Location
  ↓
dedicated Reporting worker
  ↓
claim ACCEPTED → GENERATING
  ↓
stream masked Reporting projection to CSV/JSONL
  ↓
store artifact
  ↓
AVAILABLE + recordCount + SHA-256 checksum + retrievalUri
```

A scheduled recovery loop re-dispatches durable ACCEPTED jobs after process
restart. Claiming is atomic (`ACCEPTED → GENERATING`) so duplicate dispatch does
not duplicate generation.

## Idempotency

`idempotency_key` is unique. The normalized request is SHA-256 fingerprinted.

- same key + same request: return the existing job;
- same key + different request: HTTP 409.

## Policy

The export period is explicitly bounded by required `occurredFrom` and
`occurredTo`; future-ending periods are rejected with HTTP 422.

## Artifact storage

The default adapter writes generated artifacts to a configured shared
filesystem directory and publishes a configured absolute retrieval base URI.

The deployment layer must map that URI to a protected, time-limited retrieval
mechanism. Reporting never exposes an undocumented binary-download endpoint.

## Security / audit

Request and status access require both OAuth scopes. Successful request/status
operations are themselves written to the immutable Reporting audit trail as
`AUDIT_EXPORT`.

## Not Kafka replay

Audit export uses a dedicated durable job table and worker. It does not publish
a replay command, consume a DLQ, or reuse operational Kafka replay machinery.

## Exit criteria

```text
AUDIT_EXPORT_REQUEST = IMPLEMENTED
AUDIT_EXPORT_STATUS = IMPLEMENTED
AUDIT_EXPORT_IDEMPOTENCY = IMPLEMENTED
AUDIT_EXPORT_ASYNC_GENERATION = IMPLEMENTED
AUDIT_EXPORT_RECOVERY = IMPLEMENTED
NEXT_LOT = 6.6_SECURITY_OBSERVABILITY_ACCEPTANCE
```
