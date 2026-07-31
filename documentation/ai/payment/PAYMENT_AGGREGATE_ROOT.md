# SIXPAY CONNECT — Payment Aggregate Root

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.6 — Domain Events`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `DOMAIN_EVENT_MODEL_PREPARED`  
> **Code generation:** **FORBIDDEN**

## 1. Aggregate decision

`Payment` is the sole write Aggregate Root for one logical TRESOR PAY payment
intention.

Its normative behavioural documents now include:

- Value Objects;
- snapshots and minimized evidence;
- 76 invariants;
- 16 commands and 17 operations;
- 17 statuses and 38 transitions;
- 33 Payment domain events.

## 2. Domain event contract

The future Payment domain defines a sealed conceptual family:

```text
PaymentDomainEvent extends DomainEvent
```

Every event carries:

```text
eventId
paymentId
paymentReference
correlationId
aggregateVersion
eventSequence
causationId?
occurredAt
```

`DomainEvent.eventType()` remains the event record's simple class name.

## 3. Event registration

Each successful real mutation:

1. validates all invariants;
2. computes and applies one complete next aggregate state;
3. increments `businessVersion` once;
4. registers one or more immutable Payment events;
5. assigns one-based `eventSequence` in registration order.

All events from the mutation carry the same resulting aggregate version.

Invalid operations, no-op replay, evidence replay, conflicts, stale commands
and reconstitution register no event.

## 4. Event publication

Payment only registers domain events in memory through the shared AggregateRoot
mechanism.

The application/infrastructure layer:

- releases pending events;
- maps each event to the existing `IntegrationEventEnvelope`;
- serializes an explicit safe payload;
- persists audit and Outbox atomically with Payment state;
- publishes at least once.

Payment never publishes directly to Kafka or HTTP.

## 5. Event ownership

The Payment catalogue contains only events registered by Payment.

It excludes:

- Notification delivery events;
- Customer verification-process events;
- Accounting posting/reconciliation process events;
- raw external callbacks.

Supporting modules consume Payment process-request events and return canonical
results through Lot 2.5 commands.

## 6. Notification rule

Only these events trigger Notification:

```text
PaymentImmediateResultAvailable
PaymentFinalResultAvailable
PaymentReversalResultAvailable
```

Delivery success/failure does not return to Payment and does not affect its
financial status.

## 7. Financial replay rule

`PaymentPostingRequested` and `PaymentReversalRequested` carry stable
instruction identities.

Their consumers must use those business identities for idempotency.

Broker redelivery or Outbox replay never creates a new financial instruction.

Lookup request events are read-only.

## 8. Safe payload rule

Domain events never serialize the aggregate, snapshots or protected account
Value Objects automatically.

Event-specific payloads may include only the fields approved in
`PAYMENT_EVENT_CATALOG.yaml`.

Clear accounts, tokens, credentials, raw JWT/KYC/provider payloads, balances
and diagnostics remain forbidden.

## 9. Applicable ranges

```text
PAY-AGG-001 ... PAY-AGG-014
PAY-SNAP-001 ... PAY-SNAP-018
PAY-INV-001 ... PAY-INV-076
PAY-CMD-001 ... PAY-CMD-016
PAY-OP-001  ... PAY-OP-017
PAY-TR-001  ... PAY-TR-038
PAY-EVT-001 ... PAY-EVT-033
```

Lot 2.6 decisions are `PAY-DEC-IA1-041` through `PAY-DEC-IA1-050`.

## 10. Deferred scope

- freshness, authorization, matching and secure-instruction policies;
- Domain Services and policy interfaces;
- final acceptance/model validation.

## 11. Verdict

```text
AGGREGATE ROOT: PREPARED
VALUE OBJECTS: PREPARED
SNAPSHOTS: PREPARED
INVARIANTS: PREPARED
COMMANDS AND OPERATIONS: PREPARED
STATE MACHINE: PREPARED
DOMAIN EVENTS: PREPARED
CODE GENERATION: FORBIDDEN
```
