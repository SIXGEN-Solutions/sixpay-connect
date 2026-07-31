# SIXPAY CONNECT — Payment Domain Model

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current sub-lot:** `2.7 — Policies and Domain Services`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `POLICY_AND_DOMAIN_SERVICE_MODEL_PREPARED`  
> **Code generation:** **FORBIDDEN**

## 1. Normative hierarchy

| Topic | Normative document |
| --- | --- |
| Sources | `PAYMENT_SOURCE_BASELINE.md` |
| Baseline and language | Existing IA-1 baseline/language documents |
| Boundaries | `PAYMENT_DOMAIN_BOUNDARIES.md` |
| Aggregate Root | `PAYMENT_AGGREGATE_ROOT.md` |
| Value Objects | `PAYMENT_VALUE_OBJECT_CATALOGUE.md` |
| Snapshots | `PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md` |
| Invariants | `PAYMENT_INVARIANT_CATALOGUE.md` and `.yaml` |
| Commands/operations | `PAYMENT_COMMAND_CATALOGUE.md` and `.yaml` |
| State machine | `PAYMENT_STATE_MACHINE.yaml` |
| Events | `PAYMENT_DOMAIN_EVENT_CATALOGUE.md` and `PAYMENT_EVENT_CATALOG.yaml` |
| Policies/services | `PAYMENT_POLICY_DOMAIN_SERVICE_CATALOGUE.md` and `.yaml` |
| Final validation | Lot 2.8 pending |

## 2. Current model counts

```text
1 Aggregate Root
17 statuses
4 terminal statuses
16 commands
17 aggregate operations
38 transitions
76 invariants
33 domain events
14 policies
4 pure Domain Services
12 versioned policy profiles
```

## 3. Decision architecture

```text
Canonical input + Payment state + explicit time + approved profile
        ↓
pure policy / pure Domain Service
        ↓ immutable typed decision
Payment Aggregate Root
        ↓
state mutation + ordered events
```

## 4. Domain Service boundary

A Domain Service:

- has no state;
- performs no I/O;
- has no framework dependency;
- returns a decision;
- never owns a transaction;
- never mutates Payment;
- never publishes an event.

Persistent matching, configuration loading and external execution are ports or
processes, not Domain Services.

## 5. Generation status

```text
MODEL DEFINITION: COMPLETE THROUGH LOT 2.7
FINAL VALIDATION: PENDING LOT 2.8
CODE GENERATION: FORBIDDEN
```
