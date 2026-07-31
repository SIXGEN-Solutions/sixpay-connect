# SIXPAY CONNECT — Payment Domain Model

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current sub-lot:** `2.4 — Invariants`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `INVARIANT_MODEL_PREPARED`  
> **Code generation:** **FORBIDDEN**

## 1. Normative model hierarchy

| Topic | Normative document |
| --- | --- |
| Sources | `PAYMENT_SOURCE_BASELINE.md` |
| Gate baseline | `PAYMENT_IA1_BASELINE.md` |
| Language | `PAYMENT_UBIQUITOUS_LANGUAGE.md` |
| Boundaries | `PAYMENT_DOMAIN_BOUNDARIES.md` |
| Aggregate Root | `PAYMENT_AGGREGATE_ROOT.md` |
| Identifiers and Value Objects | `PAYMENT_VALUE_OBJECT_CATALOGUE.md` |
| Snapshots and evidence | `PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md` |
| Invariants — human | `PAYMENT_INVARIANT_CATALOGUE.md` |
| Invariants — machine | `PAYMENT_INVARIANT_CATALOGUE.yaml` |
| Posting reference | `PAYMENT_BANK_POSTING_REFERENCE_DECISION.md` |
| Commands/operations | Lot 2.5 pending |
| Domain Events | Lot 2.6 pending |
| Policies/Domain Services | Lot 2.7 pending |
| Final validation | Lot 2.8 pending |

## 2. Aggregate model

```text
Payment
├── immutable identity and original intent
├── current lifecycle state
├── current accepted evidence snapshots
├── current protected bank references
├── current relevant PaymentFailure?
├── temporal metadata
└── businessVersion
```

## 3. Invariant model

The model now contains 76 complete cross-object and lifecycle invariants in
eight families:

1. identity and immutable intent;
2. boundary and confidentiality;
3. admission and authorization;
4. banking, funds and Treasury resolution;
5. posting and financial safety;
6. notification and TFJ;
7. reversal and terminal failures;
8. replay, concurrency and transaction atomicity.

## 4. State-machine relationship

`PAYMENT_STATE_MACHINE.yaml` remains an IA-0P input pending Lot 2.5
reconciliation.

The invariant catalogue already fixes these target semantics:

- banking verification and funds control are distinct;
- notification intent is orthogonal;
- one logical posting exists per Payment;
- `FAILED` requires proven absence of financial effect;
- finality requires uniquely matched TFJ `INTEGRATED`.

## 5. Generation status

```text
AGGREGATE ROOT: PREPARED
VALUE OBJECTS: PREPARED
SNAPSHOTS: PREPARED
INVARIANTS: PREPARED
COMMANDS AND OPERATIONS: PENDING LOT 2.5
CODE GENERATION: FORBIDDEN
```
