# SIXPAY CONNECT — Phase 3 / Lot 3.4

## Outbox Foundation

| Metadata | Value |
| --- | --- |
| Authoritative branch | `feat/backend-payment` |
| Golden Module | `partner` |
| Transport | None |
| Kafka publication | Forbidden |
| Relay/scheduler | Forbidden |

## Purpose

Lot 3.4 creates the durable, transport-neutral Transactional Outbox
foundation for Payment:

```text
PaymentOutboxEntity
PaymentOutboxRepository
PaymentDomainEventMapper
PaymentIntegrationMapper
```

## Golden Module alignment

The implementation reuses Partner conventions:

- PostgreSQL JSONB payload;
- `PENDING`, `PROCESSING`, `PUBLISHED`, `FAILED`, `DEAD`;
- retry and claim metadata;
- `FOR UPDATE SKIP LOCKED`;
- shared `IntegrationEventEnvelope`;
- Spring Boot 4 module auto-configuration.

## Mapping pipeline

```text
PaymentDomainEvent
  → PaymentDomainEventMapper
  → PaymentOutboxEntity
  → PaymentIntegrationMapper
  → IntegrationEventEnvelope
```

The final envelope remains transport-neutral. No Kafka API is referenced.

## Transactional rule

Future orchestration must persist the following in one transaction:

```text
Payment state + Audit + Outbox
```

`PaymentOutboxAtomicityIT` verifies commit and rollback of Payment and Outbox
together.

## Explicitly deferred

- Kafka topics and `KafkaTemplate`;
- publisher or relay;
- scheduler;
- retry execution;
- dead-letter publication;
- application services.

## Validation

From `backend/`:

```powershell
mvnw.cmd clean verify -pl payment -am
mvnw.cmd clean verify -Pfull-tests -pl payment -am
```
