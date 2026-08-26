# Phase 6 — Lot 6.4 Payment Timeline & Audit Query

## Scope

Implements the three read operations from
`payment-audit-query-api-v1.yaml`:

- `GET /internal/api/v1/payments/{paymentId}/timeline`
- `GET /internal/api/v1/payment-audit-records`
- `GET /internal/api/v1/payment-audit-records/{auditId}`

Audit export remains excluded until Lot 6.5.

## Architecture

```text
PaymentAuditQueryController
        ↓
GetPaymentTimelineUseCase /
SearchPaymentAuditRecordsUseCase /
GetPaymentAuditRecordUseCase
        ↓
PaymentAuditQueryService
        ↓
PaymentAuditReadPort
        ↓
PaymentAuditProjectionReadAdapter
        ↓
reporting_payment_audit_evidence
```

Reporting reads only its own projection. It does not query Payment, Customer,
Accounting or Notification persistence and does not load their aggregates.

## Evidence categories

The timeline supports the contract categories:

- DOMAIN
- BANKING_VERIFICATION
- ACCOUNTING
- NOTIFICATION
- TFJ
- REVERSAL

The projection is intentionally normalized and append-only.

## Stable pagination

Both paginated APIs use keyset pagination ordered by `occurred_at` plus the
evidence UUID as deterministic tie-breaker. The first request establishes a
server-owned snapshot; continuation cursors carry that snapshot in an
HMAC-SHA-256 authenticated token.

## Audit of audit access

Successful read accesses append an `AUDIT_QUERY` record to the same
Reporting-owned immutable evidence store. Export access/auditing is handled in
Lot 6.5.

## Security

All three operations require:

`SCOPE_payment.audit.read`

No financial command, mutation, replay or export endpoint is introduced.

## Persistence

Production Flyway execution remains centralized in bootstrap. Reporting keeps a
copy of the migration under test resources for module integration tests.

## Exit criteria

```text
PAYMENT_TIMELINE_QUERY = IMPLEMENTED
PAYMENT_AUDIT_SEARCH = IMPLEMENTED
PAYMENT_AUDIT_DETAIL = IMPLEMENTED
AUDIT_EXPORT = NOT_YET_IMPLEMENTED
NEXT_LOT = 6.5_CONTROLLED_AUDIT_EXPORT
```
