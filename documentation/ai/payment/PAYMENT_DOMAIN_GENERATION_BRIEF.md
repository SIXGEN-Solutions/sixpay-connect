# SIXPAY CONNECT — Payment Domain Generation Brief

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.6 — Domain Events`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `LOT_2_6_DRAFT_PENDING_VALIDATION`  
> **Code generation:** **FORBIDDEN**

## 1. Governing documents

- `ENGINEERING_CONTEXT.md`
- `PAYMENT_IA1_BASELINE.md`
- `PAYMENT_SOURCE_BASELINE.md`
- `PAYMENT_UBIQUITOUS_LANGUAGE.md`
- `PAYMENT_DOMAIN_BOUNDARIES.md`
- `PAYMENT_AGGREGATE_ROOT.md`
- `PAYMENT_VALUE_OBJECT_CATALOGUE.md`
- `PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md`
- `PAYMENT_INVARIANT_CATALOGUE.md`
- `PAYMENT_INVARIANT_CATALOGUE.yaml`
- `PAYMENT_COMMAND_CATALOGUE.md`
- `PAYMENT_COMMAND_CATALOGUE.yaml`
- `PAYMENT_STATE_MACHINE.yaml`
- `PAYMENT_DOMAIN_EVENT_CATALOGUE.md`
- `PAYMENT_EVENT_CATALOG.yaml`
- `PAYMENT_DOMAIN_MODEL.md`
- `AI_CONTEXT_MANIFEST.yaml`

## 2. Prepared model

Completed:

- Aggregate Root;
- identifiers and Value Objects;
- snapshots and minimized evidence;
- invariants;
- commands and aggregate operations;
- IA-1 state machine;
- Payment domain event catalogue.

Pending:

- policies and Domain Services;
- final acceptance/model validation.

## 3. Event model

The Payment Aggregate Root registers 33 stable event types.

Every event has:

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

The integration layer maps it explicitly to
`IntegrationEventEnvelope`.

No automatic aggregate/snapshot serialization is permitted.

## 4. Publication

```text
Payment mutation
+ immutable audit
+ Outbox event records
= one database transaction
```

Publication is at least once. Consumers deduplicate with `eventId`.

All events from one aggregate mutation share the resulting aggregate version
and use `eventSequence` for deterministic local order.

## 5. Event ownership

The catalogue contains only Payment aggregate events.

Notification, Customer and Accounting own their process events.

Raw external callbacks are mapped to canonical commands and snapshots before
Payment sees them.

## 6. Notification triggers

Only:

```text
PaymentImmediateResultAvailable
PaymentFinalResultAvailable
PaymentReversalResultAvailable
```

can create a `NotificationDelivery`.

Notification delivery never mutates Payment.

## 7. Financial process requests

Posting and reversal request events carry stable instruction identities.

Consumers reuse:

```text
instructionId
idempotencyKey
instructionFingerprint
```

They never derive a replacement financial instruction from broker redelivery.

Outcome-lookup events permit read-only lookup only.

## 8. Event catalogue ranges

```text
PAY-EVT-001 ... PAY-EVT-033
PAY-EVT-RULE-001 ... PAY-EVT-RULE-012
PAY-DEC-IA1-041 ... PAY-DEC-IA1-050
```

## 9. Lot 2.6 consistency corrections

No state or transition was added.

Existing transitions now register missing result intents for:

- indeterminate banking verification;
- indeterminate funds control;
- recoverable pre-financial failures;
- debit-only posting outcomes;
- conclusive rejected/not-allowed reversal outcomes.

Successful reversal also registers explicit terminal `PaymentReversed`.

## 10. Deferred scope

| Lot | Subject |
| --- | --- |
| 2.7 | Policies and Domain Services |
| 2.8 | Final model, event, acceptance and traceability validation |

## 11. Authorized Lot 2.6 modifications

```text
documentation/ai/payment/PAYMENT_DOMAIN_EVENT_CATALOGUE.md
documentation/ai/payment/PAYMENT_EVENT_CATALOG.yaml
documentation/ai/payment/PAYMENT_COMMAND_CATALOGUE.md
documentation/ai/payment/PAYMENT_COMMAND_CATALOGUE.yaml
documentation/ai/payment/PAYMENT_STATE_MACHINE.yaml
documentation/ai/payment/PAYMENT_VALUE_OBJECT_CATALOGUE.md
documentation/ai/payment/PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md
documentation/ai/payment/PAYMENT_AGGREGATE_ROOT.md
documentation/ai/payment/PAYMENT_DOMAIN_MODEL.md
documentation/ai/payment/PAYMENT_DOMAIN_GENERATION_BRIEF.md
documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml
```

No Java implementation, contract, architecture or requirement file is
modified.

## 12. Verdict

```text
IA-1 LOT 2.6 PAYMENT DOMAIN EVENTS PREPARED
EVENT COUNT: 33
STATUS: DRAFT_PENDING_VALIDATION
NEXT: LOT 2.7 — POLICIES AND DOMAIN SERVICES
CODE GENERATION: FORBIDDEN
```
