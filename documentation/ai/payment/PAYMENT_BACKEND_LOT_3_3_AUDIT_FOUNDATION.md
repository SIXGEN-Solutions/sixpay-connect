# SIXPAY CONNECT — Phase 3 / Lot 3.3 — Audit Foundation

Authoritative branch: `feat/backend-payment`

## Scope

This lot adds append-only Payment audit persistence only:

- `PaymentAuditEntity`
- `PaymentAuditRepository`
- `PaymentAuditAdapter`
- Flyway migration
- rollback and atomicity tests

No application service, controller, outbox, broker publication, scheduler or
external adapter is created.

## Audit payload

Only safe metadata already exposed by `PaymentDomainEvent` is persisted:
event ID, Payment identity, public reference, event type, status, business
version, event sequence, correlation ID, optional causation ID and occurrence
time.

Account references, authorization tokens, KYC data, evidence snapshots and bank
payloads are excluded.

## Atomicity

Audit writes use `Propagation.MANDATORY`. They cannot start an independent
transaction. The integration test proves:

```text
commit   -> Payment + audit are both visible
rollback -> Payment + audit are both absent
```

## Immutability

The domain event ID is the audit primary key. PostgreSQL rejects UPDATE and
DELETE through triggers. A duplicate event replay is a no-op.

## Validation

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress -pl payment -am test
mvn --batch-mode --no-transfer-progress -pl payment -am -Pfull-tests verify
```
