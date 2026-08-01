# SIXPAY CONNECT — Payment Module

## Current increment

```text
Lot 3.10 — Final Domain Kernel Validation
Scope: PAYMENT_DOMAIN_ONLY
Global generation: FORBIDDEN
```

## Implemented domain program

```text
1 Payment Aggregate Root
1 immutable PaymentState
17 named operations
17 states
38 legal transitions
33 explicit Payment domain events
14 pure policies
12 immutable policy profiles
4 pure Domain Services
```

## Aggregate ownership

`Payment` is the sole owner of:

- lifecycle transitions;
- business version and timestamps;
- bounded accepted evidence;
- Payment failure and finality;
- ordered domain-event registration.

Each successful operation prepares a complete immutable next state and its
complete event batch before atomic in-memory commit. An invalid transition,
conflict or identical replay leaves state, version and pending events unchanged.

## Event model

Events implement `PaymentDomainEvent`, extending the shared
`DomainEvent`. Every event carries:

```text
eventId (UUID v4)
paymentId
paymentReference
correlationId
paymentStatus
aggregateVersion
eventSequence
causationId?
occurredAt
```

Only explicit safe payload records are used. Full aggregates and snapshots are
never event fields.

## Deliberately absent

```text
application handlers
repositories
JPA mappings
database migrations
Outbox mapping
controllers
external adapters
Spring configuration
```

## Validate

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress -pl payment -am test
mvn --batch-mode --no-transfer-progress clean verify
```

## Lot 3.10 validation

The existing domain behavior is not duplicated. The final gate verifies the
17/17/38/76/33/14/4 counts, terminal-state protection, PAY-* traceability and
the canonical `domain.exception` package.
