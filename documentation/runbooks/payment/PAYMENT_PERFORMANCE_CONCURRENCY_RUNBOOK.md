# Payment Performance & Concurrency Runbook

## Execute the default suite

From `backend/`:

```powershell
mvnw.cmd clean verify -Pfull-tests -pl payment -am
```

## Execute only the concurrency integration test

```powershell
mvnw.cmd -pl payment \
  -Dtest=PaymentConcurrencyPerformanceIT \
  test
```

## Increase load locally

```powershell
mvnw.cmd -pl payment \
  -Dtest=PaymentConcurrencyPerformanceIT \
  -Dsixpay.payment.performance.volume=10000 \
  -Dsixpay.payment.performance.replays=500 \
  test
```

## Expected invariants

### Optimistic locking

- exactly one concurrent entity update commits;
- the stale update fails;
- `persistence_version` increments once;
- no last-write-wins overwrite occurs.

### Idempotent replay

- one request owns the key;
- every later identical request replays;
- a single durable row exists;
- the financial operation is not duplicated.

### Volume

- all generated Payment IDs are unique;
- all external references are unique;
- no partial committed batch is accepted as success.

## Failure diagnosis

### Hikari timeout

Reduce caller count or increase the test-only Hikari pool. Do not increase the
production pool without database-capacity analysis.

### Optimistic-lock test unexpectedly succeeds twice

Verify `@Version` remains on `persistenceVersion`, Hibernate schema validation
is active and each update uses a distinct persistence context.

### Replay produces more than one NEW

Verify callers enter `executeLocked` inside an active transaction and that the
same normalized operation/key pair is used.

### Virtual-thread assertion fails

Verify the test runs on Java 21 or later. This result does not depend on Spring
Boot configuration.

### Volume test is slow

Inspect PostgreSQL container resources, Docker disk performance and host CPU.
The CI timeout is a regression guard, not a production SLA.
