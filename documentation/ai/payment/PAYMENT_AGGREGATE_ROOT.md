# SIXPAY CONNECT — Payment Aggregate Root

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.7 — Policies and Domain Services`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `POLICY_AND_DOMAIN_SERVICE_MODEL_PREPARED`  
> **Code generation:** **FORBIDDEN**

## 1. Aggregate ownership

`Payment` remains the sole owner of:

- current Payment state;
- legal transitions;
- bounded evidence;
- business version;
- failure state;
- timestamps;
- Payment domain-event registration.

No policy or Domain Service can mutate or publish on behalf of Payment.

## 2. Decision dependencies

Payment operations receive an explicit `PaymentPolicyBundle` or the narrowly
required policies/services.

The bundle contains immutable policy implementations and approved effective
profiles. It contains no repository, adapter, credential, network client or
framework object.

## 3. Direct policy use

The aggregate directly uses pure policies for:

```text
authorization evidence acceptance
banking verification acceptance
funds control acceptance
Treasury resolution acceptance
evidence replay/replacement
posting instruction authorization
failure classification
```

These decisions remain close to one operation and one evidence category.

## 4. Domain Service use

Cross-object financial interpretations use:

```text
PostingOutcomeDecisionService
EndOfDayDecisionService
ReversalDecisionService
PaymentResultIntentService
```

Each service returns one immutable decision. The aggregate then:

1. verifies that the decision is compatible with its current state;
2. applies the complete mutation;
3. increments businessVersion once;
4. registers ordered domain events.

## 5. Explicit time

Payment and all policies use the same `decisionAt` supplied by the handler.

The domain never calls the system clock.

## 6. Configuration

Policy profiles are:

- approved;
- versioned;
- effective-dated;
- immutable for one decision;
- free of secrets and protected accounts.

The aggregate does not load profiles. Their identity/version is auditable when
it materially affects a decision.

## 7. External components

Repository, JWT verification, Amplitude calls, Treasury configuration access,
TFJ matching, Notification, Outbox, retries and DLQ remain outside Payment.

They provide canonical inputs or consume events.

## 8. Applicable ranges

```text
PAY-INV-001 ... PAY-INV-076
PAY-CMD-001 ... PAY-CMD-016
PAY-OP-001  ... PAY-OP-017
PAY-TR-001  ... PAY-TR-038
PAY-EVT-001 ... PAY-EVT-033
PAY-POL-001 ... PAY-POL-014
PAY-DS-001  ... PAY-DS-004
```

Lot 2.7 decisions are `PAY-DEC-IA1-051` through `PAY-DEC-IA1-060`.

## 9. Deferred scope

Only final cross-document, acceptance and generation-readiness validation
remains in Lot 2.8.

## 10. Verdict

```text
AGGREGATE ROOT: PREPARED
VALUE OBJECTS: PREPARED
SNAPSHOTS: PREPARED
INVARIANTS: PREPARED
COMMANDS AND OPERATIONS: PREPARED
STATE MACHINE: PREPARED
DOMAIN EVENTS: PREPARED
POLICIES: PREPARED
DOMAIN SERVICES: PREPARED
CODE GENERATION: FORBIDDEN
```
