# Phase 6 — Acceptance Scenario Catalog

| ID | Scenario | Expected |
|---|---|---|
| P6-A01 | Search Payment Query with valid `payment.read` | 200, stable masked projection |
| P6-A02 | Filter Payment Query by `observedCustomerId` | linked Payment rows returned without Customer persistence access |
| P6-A03 | ObservedCustomer first page then cursor continuation | same server-owned snapshot restored from signed cursor |
| P6-A04 | ObservedCustomer query without scope | request denied |
| P6-A05 | Read Payment timeline | immutable normalized evidence page |
| P6-A06 | Search Payment audit records with bounded period | stable keyset page |
| P6-A07 | Read one unknown audit record | 404 Problem Detail |
| P6-A08 | Audit API without `payment.audit.read` | request denied |
| P6-A09 | Request export with both scopes, purpose and idempotency key | durable `ACCEPTED`, 202 and `Location` |
| P6-A10 | Replay same export request with same Idempotency-Key | same export job, no duplicate dispatch |
| P6-A11 | Reuse Idempotency-Key with different export request | 409 Problem Detail |
| P6-A12 | Generate export | `GENERATING → AVAILABLE`, checksum/count/retrieval metadata; no Kafka replay |

## Acceptance invariants

- Query APIs are read-only projections.
- Reporting never loads Payment/Customer/Accounting/Notification aggregates.
- Internal API access is correlated.
- Audit evidence and export content are allow-listed and masked.
- Audit access/export operations are themselves auditable.
- Export generation is asynchronous only after durable acceptance.
- No audit export flow reuses Kafka replay or DLQ replay mechanisms.
