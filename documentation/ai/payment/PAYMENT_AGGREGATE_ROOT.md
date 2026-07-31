# SIXPAY CONNECT — Payment Aggregate Root

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.4 — Invariants`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `INVARIANT_MODEL_PREPARED`  
> **Code generation:** **FORBIDDEN**

## 1. Aggregate decision

`Payment` remains the sole write Aggregate Root for one logical TRESOR PAY
payment intention.

Normative model documents:

- `PAYMENT_VALUE_OBJECT_CATALOGUE.md`;
- `PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md`;
- `PAYMENT_INVARIANT_CATALOGUE.md`;
- `PAYMENT_INVARIANT_CATALOGUE.yaml`.

## 2. State composition

Payment contains:

- immutable identity and original-intent Value Objects;
- current `PaymentStatus`;
- at most one current accepted snapshot per evidence category;
- protected Treasury and posting references when known;
- at most one current relevant `PaymentFailure`;
- temporal fields and `businessVersion`.

Full histories, notification delivery, unmatched TFJ evidence and technical
attempts remain outside the aggregate.

## 3. Invariant enforcement

Every named aggregate operation follows this order:

```text
validate command identity/version
validate source state
validate required evidence and bindings
validate transition-specific invariants
compute next immutable state
register the domain fact
return the complete mutation
```

An invariant violation leaves the aggregate exactly unchanged.

There is no public generic status setter and no partially applied mutation.

## 4. Core aggregate invariants

- immutable Payment identity and original financial intent;
- source-scoped external-reference uniqueness;
- positive balanced Money and allocations;
- authorization before banking verification;
- banking verification before funds control;
- favorable fresh funds and protected CUT resolution before posting;
- one logical posting per Payment;
- unknown outcome resolved by lookup, never blind replay;
- completed posting distinct from TFJ finality;
- notification delivery independent from financial state;
- uniquely matched TFJ required for Treasury finality;
- reversal authorized separately on the original Payment;
- terminal states immutable;
- state, audit and Outbox intent atomic.

The complete 76-invariant catalogue is normative.

## 5. State/evidence coherence

| Condition | Required aggregate evidence |
| --- | --- |
| Received | Complete immutable request intent |
| Authorization approved | Approved authorization snapshot |
| Banking verified | Approved authorization + banking `VERIFIED` |
| Approved for posting | Banking `VERIFIED` + funds `VERIFIED` + Treasury `RESOLVED` |
| Posting submitted | One durable posting instruction identity |
| Debit-only | Debit succeeded, CUT not succeeded, principal posting reference |
| CUT credited | Both legs succeeded, principal posting reference |
| Outcome unknown | Canonical unknown/incomplete posting or reversal evidence |
| Pending TFJ | Completed posting, no final matched TFJ |
| Treasury integrated | Uniquely matched TFJ `INTEGRATED` |
| Reversal required/pending | Confirmed effect + explicit authorization as applicable |
| Reversed | Authoritative reversal success |
| Rejected | Conclusive pre-financial rejection |
| Failed | Proven absence of financial effect |

## 6. IA-0P state-machine compatibility

The IA-0P machine remains an input until Lot 2.5.

For IA-1:

- funds control is separate from banking verification;
- notification intent is orthogonal and not a target financial status;
- TFJ provider failure does not automatically imply Payment `FAILED`;
- `FAILED` requires proven absence of financial effect.

Lot 2.5 will reconcile states, operations and transitions without weakening
these invariants.

## 7. Applicable invariant identifiers

- aggregate structure: `PAY-AGG-001` to `PAY-AGG-014`;
- snapshots: `PAY-SNAP-001` to `PAY-SNAP-018`;
- complete cross-object/lifecycle catalogue: `PAY-INV-001` to `PAY-INV-076`;
- Lot 2.4 decisions: `PAY-DEC-IA1-027` to `PAY-DEC-IA1-032`.

## 8. Deferred scope

- exact aggregate operations and command signatures: Lot 2.5;
- domain-event names and safe payloads: Lot 2.6;
- policies and Domain Services: Lot 2.7;
- final cross-document validation: Lot 2.8.

## 9. Verdict

```text
AGGREGATE ROOT: PREPARED
VALUE OBJECTS: PREPARED
SNAPSHOTS: PREPARED
INVARIANTS: PREPARED
CODE GENERATION: FORBIDDEN
```
