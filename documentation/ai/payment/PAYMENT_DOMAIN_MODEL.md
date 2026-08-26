# SIXPAY CONNECT — Payment Domain Model

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `3.5 — Aggregate Root Payment and Domain Events`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `IMPLEMENTED`  
> **Global code generation:** **FORBIDDEN**  
> **Current domain-only increment:** **AUTHORIZED_AND_COMPLETED**

## Normative model

| Topic | Normative document |
| --- | --- |
| Aggregate Root | `PAYMENT_AGGREGATE_ROOT.md` |
| Value Objects | `PAYMENT_VALUE_OBJECT_CATALOGUE.md` |
| Snapshots | `PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md` |
| Invariants | `PAYMENT_INVARIANT_CATALOGUE.md` and `.yaml` |
| Commands/operations | `PAYMENT_COMMAND_CATALOGUE.md` and `.yaml` |
| State machine | `PAYMENT_STATE_MACHINE.yaml` |
| Events | `PAYMENT_DOMAIN_EVENT_CATALOGUE.md` and `PAYMENT_EVENT_CATALOG.yaml` |
| Policies/services | `PAYMENT_POLICY_DOMAIN_SERVICE_CATALOGUE.md` and `.yaml` |
| Acceptance | `PAYMENT_ACCEPTANCE_SCENARIOS.md` |
| Final validation | `PAYMENT_MODEL_VALIDATION_REPORT.md` and `.yaml` |

## Final counts

```text
1 Aggregate Root
17 states
4 terminal states
16 commands
17 aggregate operations
38 transitions
76 invariants
33 events
14 policies
4 Domain Services
12 policy profiles
174 named acceptance scenarios
```

## Final architecture

```text
canonical input + Payment state + explicit time + approved profile
        ↓
pure policy / pure Domain Service
        ↓ immutable typed decision
Payment Aggregate Root
        ↓
state mutation + business version + ordered events
        ↓
explicit safe Outbox mapping
```

## Validation verdict

All internal model checks pass after correcting the two obsolete generic
unknown-outcome references.

No unresolved decision blocks the domain-model shape.

Global code generation remains disabled because contract and owner approvals
are external gate prerequisites.

```text
PAYMENT IA-1 MODEL: FINAL_VALIDATED_AND_FROZEN
LOT 2: COMPLETE
GENERATION READINESS: READY_PENDING_EXTERNAL_APPROVALS
CODE GENERATION: FORBIDDEN_PENDING_EXPLICIT_APPROVAL
```


## Lot 3.5 implementation status

```text
Aggregate Root: IMPLEMENTED
Immutable aggregate state: IMPLEMENTED
17 named operations: IMPLEMENTED
17 statuses / 38 transitions: ENFORCED
33 domain events: IMPLEMENTED
14 policies / 4 Domain Services: INTEGRATED
Application, persistence and adapters: NOT AUTHORIZED
```

The Payment domain-only implementation is complete. Global generation remains
blocked by the external contract and configuration approvals already recorded
in the manifest.
