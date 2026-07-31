# SIXPAY CONNECT — Payment Domain Generation Brief

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `3.3 — Snapshots and financial evidence`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `LOT_3_3_IMPLEMENTED`  
> **Global code generation:** **FORBIDDEN**  
> **Current increment:** **AUTHORIZED**

## Authorization

```text
PAY-AUTH-IA1-001
scope: PAYMENT_DOMAIN_ONLY
currentIncrement: LOT_3_3_SNAPSHOTS_FINANCIAL_EVIDENCE
currentIncrementCodeGenerationAllowed: true
futureIncrementActivationRequired: true
globalCodeGenerationAllowed: false
```

## Implemented snapshots

```text
AuthorizationEvidenceSnapshot
BankingVerificationSnapshot
FundsControlSnapshot
TreasuryAccountResolutionSnapshot
PostingOutcomeSnapshot
EndOfDayConfirmationSnapshot
ReversalSnapshot
```

## Implemented support

The package includes metadata, identifiers, observation channels,
authorization bindings, banking/funds checks, posting legs, TFJ failure
evidence and reversal authorization/outcome evidence.

Primitive posting/reversal instruction IDs and idempotency keys are included
because the snapshots require stable financial-command identity. Full
instruction composites remain deferred.

## Structural validation

Constructors enforce bounded immutable collections, source/channel
compatibility, chronology, outcome matrices, positive amounts, posting
leg/reference consistency, TFJ final-status structure and reversal identity
preservation.

## Deferred behavior

```text
freshness profiles
mandatory check profiles
aggregate bank/account/amount bindings
replay and replacement authority
TFJ unique match proof
state transition eligibility
Payment mutation
domain event registration
```

## Verdict

```text
LOT 3.3: IMPLEMENTED
SNAPSHOTS: 7
MODEL SEMANTICS: UNCHANGED
GLOBAL GENERATION: FORBIDDEN
NEXT: LOT 3.4 EXPLICIT ACTIVATION
```
