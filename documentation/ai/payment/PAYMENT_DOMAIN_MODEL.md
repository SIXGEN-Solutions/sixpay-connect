# SIXPAY CONNECT — Payment Domain Model

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current sub-lot:** `2.3 — Snapshots and Business Evidence`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `SNAPSHOT_MODEL_PREPARED`  
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
| Posting reference | `PAYMENT_BANK_POSTING_REFERENCE_DECISION.md` |
| Invariants | Lot 2.4 pending |
| Commands/operations | Lot 2.5 pending |
| Domain Events | Lot 2.6 pending |
| Policies/Domain Services | Lot 2.7 pending |
| Final validation | Lot 2.8 pending |

## 2. Current aggregate composition

```text
Payment
├── immutable identity and request Value Objects
├── PaymentStatus
├── current decision evidence
│   ├── AuthorizationEvidenceSnapshot?
│   ├── BankingVerificationSnapshot?
│   ├── FundsControlSnapshot?
│   ├── TreasuryAccountResolutionSnapshot?
│   ├── PostingOutcomeSnapshot?
│   ├── EndOfDayConfirmationSnapshot?
│   └── ReversalSnapshot?
├── TreasuryAccountReference?
├── BankPostingReference?
├── current PaymentFailure?
├── timestamps
└── businessVersion
```

## 3. Evidence boundary

Snapshots are canonical minimized evidence, not provider payloads.

They include common metadata:

```text
source
correlation
observation channel
evidence fingerprint
observedAt
acceptedAt
```

Full historical versions belong to append-only audit/reporting.

## 4. Key snapshot semantics

- authorization stores safe binding results, not JWT/claims;
- banking verification stores canonical checks, not KYC/customer payload;
- funds control binds exact amount/account and excludes available balance;
- Treasury resolution proves protected bank configuration;
- posting stores command identity, financial outcome and both leg results;
- TFJ stores only uniquely matched final results;
- reversal combines immutable authorization proof and optional outcome.

## 5. Generation status

```text
AGGREGATE ROOT: PREPARED
VALUE OBJECTS: PREPARED
SNAPSHOTS: PREPARED
INVARIANTS: PENDING LOT 2.4
CODE GENERATION: FORBIDDEN
```
