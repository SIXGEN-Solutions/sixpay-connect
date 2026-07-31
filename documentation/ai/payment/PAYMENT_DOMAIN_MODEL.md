# SIXPAY CONNECT — Payment Domain Model

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current sub-lot:** `2.6 — Domain Events`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `DOMAIN_EVENT_MODEL_PREPARED`  
> **Code generation:** **FORBIDDEN**

## 1. Normative hierarchy

| Topic | Normative document |
| --- | --- |
| Sources | `PAYMENT_SOURCE_BASELINE.md` |
| Gate baseline | `PAYMENT_IA1_BASELINE.md` |
| Language | `PAYMENT_UBIQUITOUS_LANGUAGE.md` |
| Boundaries | `PAYMENT_DOMAIN_BOUNDARIES.md` |
| Aggregate Root | `PAYMENT_AGGREGATE_ROOT.md` |
| Value Objects | `PAYMENT_VALUE_OBJECT_CATALOGUE.md` |
| Snapshots | `PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md` |
| Invariants | `PAYMENT_INVARIANT_CATALOGUE.md` and `.yaml` |
| Commands/operations | `PAYMENT_COMMAND_CATALOGUE.md` and `.yaml` |
| State machine | `PAYMENT_STATE_MACHINE.yaml` |
| Domain Events — human | `PAYMENT_DOMAIN_EVENT_CATALOGUE.md` |
| Domain Events — machine | `PAYMENT_EVENT_CATALOG.yaml` |
| Policies/Domain Services | Lot 2.7 pending |
| Final validation | Lot 2.8 pending |

## 2. Current model counts

```text
1 Payment Aggregate Root
17 Payment statuses
4 terminal statuses
16 application commands
17 aggregate operations
38 legal transitions
76 complete invariants
33 Payment domain events
```

## 3. Event architecture

```text
Payment operation
    ↓
PaymentDomainEvent record(s)
    ↓ releaseDomainEvents()
explicit safe Outbox mapping
    ↓
IntegrationEventEnvelope
    ↓
durable consumer deduplication by eventId
```

All events from one mutation share `aggregateVersion` and are ordered by
`eventSequence`.

## 4. Event roles

The catalogue distinguishes:

- lifecycle and terminal facts;
- accepted evidence facts;
- financial facts;
- external process requests;
- notification result intents.

Only result-intent events trigger Notification.

## 5. State-machine corrections bound in Lot 2.6

Without adding states or transitions, the final machine now guarantees:

- indeterminate/recoverable and debit-only paths publish `PROCESSING` result
  intent;
- reversal rejected/not allowed publishes a conclusive reversal result;
- successful reversal publishes explicit `PaymentReversed`.

## 6. Generation status

```text
AGGREGATE ROOT: PREPARED
VALUE OBJECTS: PREPARED
SNAPSHOTS: PREPARED
INVARIANTS: PREPARED
COMMANDS: PREPARED
OPERATIONS: PREPARED
STATE MACHINE: PREPARED
DOMAIN EVENTS: PREPARED
POLICIES AND DOMAIN SERVICES: PENDING LOT 2.7
CODE GENERATION: FORBIDDEN
```
