# SIXPAY CONNECT — Payment Domain Generation Brief

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.4 — Invariants`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `LOT_2_4_DRAFT_PENDING_VALIDATION`  
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
- `PAYMENT_DOMAIN_MODEL.md`
- `PAYMENT_BANK_POSTING_REFERENCE_DECISION.md`
- `AI_CONTEXT_MANIFEST.yaml`
- `documentation/contracts/CONTRACT_REGISTRY.yaml`

## 2. Model readiness

Prepared:

- Aggregate Root;
- identifiers and Value Objects;
- snapshots and minimized business evidence;
- complete cross-object and lifecycle invariants.

Still pending:

- command and operation signatures;
- domain-event schemas;
- policies and Domain Services;
- final cross-document validation.

## 3. Invariant catalogue

The normative catalogue contains 76 invariants in eight families:

```text
IDENTITY_AND_IMMUTABILITY
BOUNDARY_AND_CONFIDENTIALITY
AUTHORIZATION_AND_ADMISSION
BANKING_AND_FUNDS
POSTING_AND_FINANCIAL_SAFETY
NOTIFICATION_AND_TFJ
REVERSAL_AND_FAILURE
REPLAY_CONCURRENCY_AND_ATOMICITY
```

Each invariant declares:

- statement;
- enforcement layer;
- violation result;
- source traceability;
- required verification level.

## 4. Default violation behavior

```text
reject operation
leave Payment unchanged
do not increment businessVersion
do not register a domain event
do not append a state-transition audit
do not create an Outbox intent
```

Conflicting financial or TFJ evidence is quarantined by its owning module and
does not mutate Payment.

## 5. Financial safety invariants

- only one logical posting instruction per Payment;
- exact retries reuse the same posting identity;
- unknown posting/reversal outcomes require authoritative lookup;
- no blind financial replay;
- `FAILED` requires proven absence of financial effect;
- partial debit/CUT outcomes are never success;
- posting success is distinct from TFJ finality;
- original posting identity survives reversal.

## 6. Notification and TFJ invariants

- notification is an intent/event, not delivery state;
- delivery never changes financial state;
- TFJ tracking does not wait for notification delivery;
- only authenticated, durable, uniquely matched `INTEGRATED` TFJ evidence
  establishes Treasury finality;
- pending/unmatched/conflicting TFJ evidence remains in Accounting;
- TFJ delay alone cannot trigger reversal or failure.

## 7. IA-0P machine reconciliation

Lot 2.4 does not rewrite `PAYMENT_STATE_MACHINE.yaml`.

The IA-1 invariant catalogue supersedes conflicting IA-0P semantics regarding:

- distinct banking and funds gates;
- notification orthogonality;
- `FAILED` classification;
- TFJ failure mapping;
- one-posting rule.

Lot 2.5 will reconcile final commands, states and transitions.

## 8. Closed decisions

`PAY-DEC-IA1-027` through `PAY-DEC-IA1-032` are defined in
`PAYMENT_INVARIANT_CATALOGUE.md`.

The complete invariant range is:

```text
PAY-INV-001
...
PAY-INV-076
```

## 9. Deferred scope

| Lot | Deferred subject |
| --- | --- |
| 2.5 | Commands, aggregate operations and state-machine reconciliation |
| 2.6 | Domain Events and safe payloads |
| 2.7 | Policies and Domain Services |
| 2.8 | Final model and acceptance validation |

## 10. Traceability prefixes

```text
PAY-BASE-*
PAY-BOUND-*
PAY-POSTREF-*
PAY-AGG-*
PAY-VO-*
PAY-SNAP-*
PAY-INV-*
PAY-DEC-*
PAY-CONTRACT-*
PAY-AI-*
PAY-SRC-*
SRC-*
OPEN-*
```

## 11. Authorized Lot 2.4 modifications

```text
documentation/ai/payment/PAYMENT_INVARIANT_CATALOGUE.md
documentation/ai/payment/PAYMENT_INVARIANT_CATALOGUE.yaml
documentation/ai/payment/PAYMENT_AGGREGATE_ROOT.md
documentation/ai/payment/PAYMENT_DOMAIN_MODEL.md
documentation/ai/payment/PAYMENT_DOMAIN_GENERATION_BRIEF.md
documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml
```

No implementation, contract, requirement, architecture, state-machine or event
catalogue file is modified.

## 12. Verdict

```text
IA-1 LOT 2.4 PAYMENT INVARIANTS PREPARED
INVARIANT COUNT: 76
STATUS: DRAFT_PENDING_VALIDATION
NEXT: LOT 2.5 — COMMANDS AND AGGREGATE OPERATIONS
CODE GENERATION: FORBIDDEN
```
