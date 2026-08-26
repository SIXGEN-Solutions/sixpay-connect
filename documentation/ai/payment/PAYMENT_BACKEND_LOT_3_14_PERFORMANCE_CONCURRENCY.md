# SIXPAY CONNECT — Phase 3 / Lot 3.14

## Performance & Concurrency

### Repository findings

The authoritative implementation already contains:

- Java 21;
- JPA optimistic locking through `@Version persistenceVersion`;
- conversion of optimistic-lock exceptions to
  `PaymentPersistenceException`;
- PostgreSQL transaction-scoped advisory locking for idempotency;
- durable replay records;
- PostgreSQL Testcontainers integration patterns.

No explicit repository configuration enabling Spring Boot virtual threads was
found. This lot therefore validates Java 21 virtual-thread capability and uses
virtual threads as load generators. It does not claim that all Spring request
handling or task execution is already virtual-thread based.

## Tests

### Virtual threads

`PaymentVirtualThreadsTest` executes 10,000 independent tasks through:

```java
Executors.newVirtualThreadPerTaskExecutor()
```

Every task asserts that its executing thread is virtual.

### Optimistic locking

`PaymentConcurrencyPerformanceIT` opens two independent JPA persistence
contexts, loads the same `PaymentJpaEntity`, commits the first update and
asserts that the second commit fails with an optimistic-lock conflict.

The database must retain only the first update and increment
`persistence_version` once.

### Strong concurrency and replay

128 virtual-thread callers execute the same operation and idempotency key.

Expected result:

```text
1 NEW
127 REPLAY
1 durable idempotency record
```

The existing PostgreSQL advisory lock serializes the critical section.

### Thousands of payments

The integration test inserts 3,000 unique Payment rows using concurrent
virtual-thread workers and JDBC batches against PostgreSQL 15.

The test verifies:

- total inserted rows;
- uniqueness of Payment IDs;
- unique external references;
- completion within a bounded test duration.

## Configurable load

The default values may be overridden:

```powershell
-Dsixpay.payment.performance.volume=10000
-Dsixpay.payment.performance.replays=500
```

The defaults remain suitable for CI and developer workstations.

## What these tests do not prove

They do not replace a production capacity test. They do not measure:

- Core Banking latency;
- Kafka throughput;
- Accounting and Notification throughput;
- Kubernetes resource limits;
- p95/p99 under production network conditions;
- database failover;
- horizontal scaling across several application instances.

A dedicated environment and workload model remain necessary before production
capacity sign-off.
