# SIXPAY CONNECT — Payment Aggregate Root

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.8 — Final Model Validation`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `FINAL_VALIDATED_AND_FROZEN`  
> **Code generation:** **FORBIDDEN_PENDING_EXPLICIT_APPROVAL**

## Final aggregate decision

`Payment` is the sole write Aggregate Root for one logical TRESOR PAY payment
intention.

It is the sole owner of:

- its 17-state lifecycle;
- legal transitions;
- bounded current evidence;
- business version and timestamps;
- failure state;
- Payment domain-event registration.

Policies and Domain Services return immutable decisions and never mutate the
aggregate.

## Final behavioural inventory

```text
16 application commands
17 aggregate operations
38 legal transitions
76 invariants
33 Payment domain events
14 policies
4 pure Domain Services
```

`PAY-OP-017 reconstitute` is intentionally commandless, transitionless and
eventless.

`SAME_AS_SOURCE` is a valid pseudo-target only for `PAY-TR-037`, where a
recoverable pre-financial failure retains the concrete source state.

## Final financial semantics

- `POSTING_OUTCOME_UNKNOWN` and `REVERSAL_OUTCOME_UNKNOWN` are distinct.
- `DEBIT_CONFIRMED` represents a known debit without complete CUT credit.
- `FAILED` requires proven absence of financial effect.
- `TREASURY_INTEGRATED` requires uniquely matched TFJ `INTEGRATED` evidence.
- `REVERSED` preserves the original posting evidence.
- Notification delivery is never a Payment state.

## Freeze and generation gate

The IA-1 aggregate model is frozen. A semantic change requires traceable change
control and re-execution of Lot 2.8 validation.

The model is ready for implementation planning, but code generation remains
disabled until explicit owner and contract-gate approval.

## Verdict

```text
PAYMENT AGGREGATE ROOT: FINAL_VALIDATED
LOT 2: COMPLETE
MODEL BLOCKERS: NONE
CODE GENERATION: FORBIDDEN_PENDING_EXPLICIT_APPROVAL
```
