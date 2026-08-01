# SIXPAY CONNECT — Payment Domain Generation Brief

> **Current lot:** `7 — Conceptual Persistence and Concurrency`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `LOT_7_IMPLEMENTED`  
> **Global code generation:** **FORBIDDEN**  
> **Current increment:** **AUTHORIZED_AND_COMPLETED**

## Implemented domain model

```text
1 Payment Aggregate Root
1 immutable PaymentState
17 named Aggregate Root operations
17 lifecycle states
38 legal transitions
33 explicit Payment domain-event records
14 pure Policies
12 immutable policy profiles
4 pure Domain Services
```

## Aggregate guarantees

- Payment is the only state, version and event owner.
- Every successful operation increments businessVersion exactly once.
- Every event in one mutation shares that resulting version.
- eventSequence is one-based in canonical fact order.
- identical replay, conflict, invalid transition and reconstitution are
  eventless and versionless.
- financial outcome uncertainty is resolved only through authoritative lookup.
- original posting evidence remains preserved through reversal.
- Notification delivery never changes Payment state.

## Still forbidden

```text
application handlers
command registry
repositories and persistence mappings
database migrations
Outbox and integration-envelope mapping
controllers and API changes
external adapters
Spring configuration
bank-specific configuration activation
```

## Verdict

```text
LOT 3.5: IMPLEMENTED
PAYMENT DOMAIN-ONLY PROGRAM: COMPLETE
GLOBAL GENERATION: FORBIDDEN
NEXT: OWNER REVIEW AND EXPLICIT NEXT-INCREMENT ACTIVATION
```


## Lot 5 invariant enforcement strategy

The 76 frozen invariants are now formalized end-to-end. Each invariant states:

- when it is checked;
- which component supplies the proof;
- which Aggregate Root operation applies it;
- the stable error code;
- the tests;
- the audit impact;
- the persistence or integration constraint;
- the split between aggregate, application, persistence, idempotency store,
  banking integration and Accounting.

The Aggregate Root remains responsible only for internal consistency and
transitions. Global uniqueness, distributed idempotence and transactional
state/audit/Outbox guarantees remain explicit responsibilities of future
vertical layers.


## Lot 6 events, audit and Outbox

The Payment Aggregate continues to register only immutable Domain Events and
never calls Kafka or an Outbox repository directly.

The future application transaction must atomically persist:

```text
Payment state and businessVersion
+ one immutable audit record per Domain Event
+ one Outbox row per publishable event
+ idempotency result when applicable
```

Publication after commit is at-least-once. Republishing preserves event
identity and payload; consumer deduplication uses `eventId`.


## Lot 7 conceptual persistence and concurrency

The Payment brief now defines persistence capabilities without fixing physical
table names or generating JPA/migrations.

Mandatory capabilities include:

```text
aggregate state and optimistic version
external/public reference uniqueness
current safe failure and optional failure history
immutable audit
request idempotency
transactional Outbox
profile-dependent authorization replay protection
authoritative posting/reversal lookup correlation
```

All eight required concurrency and duplicate-delivery scenarios have a
deterministic arbitration and stable outcome.
