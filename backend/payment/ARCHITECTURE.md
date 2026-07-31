# Architecture Decision Record — Payment Aggregate and Events

## Decision

Lot 3.5 implements the frozen IA-1 Aggregate Root and event catalogue without
opening application, persistence or integration layers.

## Atomic mutation model

```text
validate current state and immutable inputs
        ↓
invoke pure policy or Domain Service
        ↓ typed decision
construct immutable next PaymentState
        ↓
construct complete ordered event batch
        ↓
validate event metadata against next state
        ↓
commit state and pending events together
```

No event is registered until every next-state invariant and event payload has
been successfully constructed.

## Version and ordering

One successful mutation produces:

```text
businessVersion = previousVersion + 1
all events.aggregateVersion = businessVersion
events.eventSequence = 1..N
```

No-op replay, conflict, invalid transition and reconstitution produce no
version increment and no event.

## Reconstitution

`Payment.reconstitute(PaymentState)` accepts a fully validated immutable state,
restores no pending events and performs no transition.

## Event safety

The 33 event types are explicit records. Common metadata and safe payload
components are reusable, but no generic domain-event payload or automatic
aggregate/snapshot serialization exists.

## Boundary

Payment requests external processes only by domain events. It performs no
network, repository, persistence, broker, configuration or clock I/O.
