# SIXPAY CONNECT — Payment Domain Model

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current sub-lot:** `2.5 — Commands and Business Operations`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `COMMAND_AND_OPERATION_MODEL_PREPARED`  
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
| Domain Events | Lot 2.6 pending |
| Policies/Domain Services | Lot 2.7 pending |
| Final validation | Lot 2.8 pending |

## 2. Write-model composition

```text
Payment
├── immutable original intent
├── current PaymentStatus
├── current accepted evidence
├── PostingInstructionIdentity?
├── BankPostingReference?
├── ReversalSnapshot?
├── current PaymentFailure?
├── timestamps
└── businessVersion
```

## 3. Command model

The write model accepts 16 application commands mapped to 16 public mutation
operations plus the `reconstitute` factory.

Commands remain outside the aggregate. Handlers perform deduplication,
authorization, version control and transaction management.

## 4. State model

The IA-1 state machine contains:

```text
17 states
4 terminal states
38 transitions
```

Important corrections from IA-0P:

- banking verification and funds control are separate;
- `NOTIFIED` is removed;
- posting and reversal unknown outcomes are separate;
- completed posting goes directly to `POSTED_PENDING_TFJ`;
- TFJ failure does not automatically map to Payment `FAILED`.

## 5. External process boundary

Payment operations register facts that request Customer, Accounting,
Integration and Notification work.

They never call external systems directly.

## 6. Generation status

```text
AGGREGATE ROOT: PREPARED
VALUE OBJECTS: PREPARED
SNAPSHOTS: PREPARED
INVARIANTS: PREPARED
COMMANDS: PREPARED
OPERATIONS: PREPARED
STATE MACHINE: PREPARED
DOMAIN EVENTS: PENDING LOT 2.6
CODE GENERATION: FORBIDDEN
```
