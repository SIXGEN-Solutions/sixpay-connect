# SIXPAY CONNECT — Payment Domain Generation Brief

> **Current lot:** `4 — Normative State Machine`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `LOT_4_IMPLEMENTED`  
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


## Lot 4 normative state-machine artefacts

```text
PAYMENT_NORMATIVE_STATE_MACHINE.md
PAYMENT_FORBIDDEN_TRANSITION_MATRIX.md
PAYMENT_STATE_EXTERNAL_EVENT_MATRIX.md
PAYMENT_STATE_MACHINE.yaml
```

The Lot 4 documentation formalizes the existing 17-state and 38-transition
implementation. It introduces no new state or Java behavior.
