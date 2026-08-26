# SIXPAY CONNECT — Phase 3 / Lot 3.2

## Persistence Foundation

| Metadata | Value |
| --- | --- |
| Authoritative branch | `feat/backend-payment` |
| Scope | Payment persistence only |
| Domain Kernel | Frozen and unchanged |
| Application services | Forbidden |
| Controllers and adapters outside persistence | Forbidden |
| Financial external calls | Forbidden |

## Purpose

Lot 3.2 introduces the durable representation required to save and
reconstitute the complete Payment Aggregate Root. It does not orchestrate any
business workflow and does not call an external system.

## Design decisions

### Domain model remains persistence-ignorant

`Payment`, `PaymentState`, evidence snapshots, policies and Domain Services
receive no JPA or Spring annotation.

The framework-free port is:

```text
com.sixpay.payment.domain.repository.PaymentRepository
```

The implementation is isolated under:

```text
com.sixpay.payment.infrastructure.persistence
```

### Complete state, not a reduced projection

A persisted Payment must be sufficient for:

```java
Payment.reconstitute(completePaymentState)
```

The persistence model therefore stores:

1. explicit relational columns for identity, uniqueness, status, monetary
   search, timestamps and versions;
2. one versioned JSONB state document containing every field required by
   `PaymentState`.

A partial state or event replay reconstruction is outside this lot.

### Two distinct versions

```text
business_version
```

is owned by the domain and changes once per successful aggregate mutation.

```text
persistence_version
```

is owned by JPA optimistic locking and protects concurrent database writes.

They must never be merged.

### Identity constraints

The schema enforces:

```text
PRIMARY KEY(payment_id)
UNIQUE(public_payment_reference)
UNIQUE(payment_source, external_payment_reference)
```

These database constraints are authoritative under race conditions. A prior
existence check may improve diagnostics but never replaces the constraints.

### Migration location

The migration is packaged in the Payment module under:

```text
backend/payment/src/main/resources/db/migration/
```

The executable bootstrap remains responsible for production Flyway execution.
The Payment module uses Flyway directly only in integration tests.

## Created files

```text
backend/payment/src/main/java/com/sixpay/payment/domain/repository/
└── PaymentRepository.java

backend/payment/src/main/java/com/sixpay/payment/infrastructure/persistence/
├── PaymentJpaEntity.java
├── PaymentPersistenceException.java
├── PaymentPersistenceMapper.java
├── PaymentRepositoryAdapter.java
├── PaymentSpringDataRepository.java
├── PaymentStateDocument.java
└── package-info.java

backend/payment/src/main/resources/db/migration/
└── V2026080101__create_payment_persistence.sql
```

Tests cover architecture, adapter delegation and migration execution.

## Explicitly deferred

The following remain forbidden in Lot 3.2:

- application commands and use cases;
- audit tables;
- outbox tables;
- idempotency arbitration;
- posting and reversal gateways;
- TFJ inbox and reconciliation;
- REST controllers;
- security policies;
- schedulers;
- direct Kafka publication.

## Validation

Run from `backend/`:

```bash
mvn --batch-mode --no-transfer-progress -pl payment -am test
mvn --batch-mode --no-transfer-progress \
    -pl payment -am -Pfull-tests verify
```

A command is not considered successful until its exit code and test results
have been observed.
