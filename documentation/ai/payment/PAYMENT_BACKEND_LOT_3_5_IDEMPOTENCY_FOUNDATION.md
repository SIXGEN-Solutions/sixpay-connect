# SIXPAY CONNECT — Phase 3 / Lot 3.5

## Idempotency Foundation

| Metadata | Value |
| --- | --- |
| Authoritative branch | `feat/backend-payment` |
| Golden Module | `partner` |
| Database | PostgreSQL 15 |
| Hash | SHA-256 |
| Application services | Deferred |
| HTTP/API behavior | Deferred |

## Purpose

Lot 3.5 establishes durable Payment idempotency before application
orchestration and external financial calls are introduced.

The lot creates:

```text
PaymentIdempotencyEntity
PaymentIdempotencyHasher
PaymentIdempotencyReplayStore
PaymentIdempotencyConcurrencyCoordinator
```

Supporting repository, decision and conflict types are included.

## Golden Module alignment

Partner already serializes identical keys through:

```sql
pg_advisory_xact_lock(
    hashtextextended(operation || ':' || idempotency_key, 0)
)
```

Payment reuses that PostgreSQL transaction-scoped locking pattern.

Payment extends the Partner baseline because a financial operation must also
distinguish:

```text
same key + same request    → replay or in-progress result
same key + different request → conflict
new key                    → one new reservation
```

## Canonical request hashing

`PaymentIdempotencyHasher` computes lowercase SHA-256 from an already
canonicalized request representation.

The future application boundary is responsible for canonicalization. The
hasher never silently reorders fields or removes data because that could make
two materially different financial requests appear identical.

## Persistence states

```text
IN_PROGRESS
COMPLETED
FAILED
```

A completed record stores:

- Payment ID;
- response status;
- exact replayable response payload;
- completion timestamp.

A failed record can restart only under the same operation, key and request
hash.

## Concurrency rule

The application layer must execute the following in one database transaction:

```text
ConcurrencyCoordinator.executeLocked(operation, key)
    → ReplayStore.begin(...)
    → Payment mutation
    → Payment persistence
    → Audit persistence
    → Outbox persistence
    → ReplayStore.complete(...)
```

The advisory lock is released automatically at transaction completion.

An in-memory Java lock is intentionally forbidden because SIXPAY CONNECT may
run on several application instances.

## Database guarantees

The migration enforces:

```text
UNIQUE(operation, idempotency_key)
request_hash = lowercase SHA-256
completed records contain a replay response
in-progress records contain no response
failed records contain a failure reason
```

The JPA persistence version is separate from all Payment business versions.

## Tests

The lot includes:

- deterministic hash tests;
- exact replay tests;
- request-hash conflict tests;
- two concurrent transactions using the same advisory lock key.

## Explicitly deferred

- command canonicalization;
- HTTP idempotency headers;
- application exception translation;
- Payment use cases;
- retry policy;
- cleanup and retention scheduling;
- distributed cache;
- bank posting idempotency contracts.

## Validation

From the backend reactor root on Windows:

```powershell
mvnw.cmd clean verify -pl payment -am
mvnw.cmd clean verify -Pfull-tests -pl payment -am
```
