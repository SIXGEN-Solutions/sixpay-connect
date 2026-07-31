# SIXPAY CONNECT — Payment Domain Generation Brief

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.7 — Policies and Domain Services`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `LOT_2_7_DRAFT_PENDING_VALIDATION`  
> **Code generation:** **FORBIDDEN**

## 1. Governing documents

- `ENGINEERING_CONTEXT.md`
- all prior Payment IA-1 baseline, language and boundary documents;
- `PAYMENT_AGGREGATE_ROOT.md`;
- `PAYMENT_VALUE_OBJECT_CATALOGUE.md`;
- `PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md`;
- `PAYMENT_INVARIANT_CATALOGUE.md` and `.yaml`;
- `PAYMENT_COMMAND_CATALOGUE.md` and `.yaml`;
- `PAYMENT_STATE_MACHINE.yaml`;
- `PAYMENT_DOMAIN_EVENT_CATALOGUE.md`;
- `PAYMENT_EVENT_CATALOG.yaml`;
- `PAYMENT_POLICY_DOMAIN_SERVICE_CATALOGUE.md`;
- `PAYMENT_POLICY_DOMAIN_SERVICE_CATALOGUE.yaml`;
- `PAYMENT_DOMAIN_MODEL.md`;
- `AI_CONTEXT_MANIFEST.yaml`.

## 2. Lot 2.7 result

The Payment model now defines:

```text
14 policies
4 pure Domain Services
12 versioned policy-profile types
16 external components explicitly excluded from Domain Service status
```

## 3. Pure decision rule

Policies and Domain Services:

- are deterministic for the same inputs;
- receive `decisionAt` explicitly;
- receive effective profiles explicitly;
- perform no repository, network, clock, secret or configuration I/O;
- depend on no Spring/JPA/Jackson/integration DTO;
- return immutable typed decisions.

## 4. Aggregate rule

Payment remains the only component allowed to:

- change Payment state;
- increment businessVersion;
- update timestamps;
- register Payment domain events.

A service decision does not itself constitute a transition.

## 5. Profile rule

The application loads approved versioned profiles through
`PaymentPolicyConfigurationPort`.

No bank-specific rule is hard-coded or invented in the domain.

Secrets and protected account values are forbidden in policy profiles.

## 6. Financial decision services

```text
PostingOutcomeDecisionService
EndOfDayDecisionService
ReversalDecisionService
PaymentResultIntentService
```

Posting/TFJ/reversal execution, lookup, matching and persistence remain outside
the domain.

## 7. Traceability ranges

```text
PAY-POL-001 ... PAY-POL-014
PAY-DS-001  ... PAY-DS-004
PAY-DEC-IA1-051 ... PAY-DEC-IA1-060
```

The command, invariant, transition and event catalogues contain direct policy
and service references.

## 8. Authorized Lot 2.7 modifications

```text
documentation/ai/payment/PAYMENT_POLICY_DOMAIN_SERVICE_CATALOGUE.md
documentation/ai/payment/PAYMENT_POLICY_DOMAIN_SERVICE_CATALOGUE.yaml
documentation/ai/payment/PAYMENT_INVARIANT_CATALOGUE.md
documentation/ai/payment/PAYMENT_INVARIANT_CATALOGUE.yaml
documentation/ai/payment/PAYMENT_COMMAND_CATALOGUE.md
documentation/ai/payment/PAYMENT_COMMAND_CATALOGUE.yaml
documentation/ai/payment/PAYMENT_STATE_MACHINE.yaml
documentation/ai/payment/PAYMENT_DOMAIN_EVENT_CATALOGUE.md
documentation/ai/payment/PAYMENT_EVENT_CATALOG.yaml
documentation/ai/payment/PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md
documentation/ai/payment/PAYMENT_AGGREGATE_ROOT.md
documentation/ai/payment/PAYMENT_DOMAIN_MODEL.md
documentation/ai/payment/PAYMENT_DOMAIN_GENERATION_BRIEF.md
documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml
```

No Java, database, API contract, architecture or requirement file is modified.

## 9. Deferred scope

Lot 2.8 performs final cross-catalogue validation, acceptance coverage,
open-decision review and generation-readiness verdict.

## 10. Verdict

```text
IA-1 LOT 2.7 POLICIES AND DOMAIN SERVICES PREPARED
POLICY COUNT: 14
DOMAIN SERVICE COUNT: 4
STATUS: DRAFT_PENDING_VALIDATION
NEXT: LOT 2.8 — FINAL MODEL VALIDATION
CODE GENERATION: FORBIDDEN
```
