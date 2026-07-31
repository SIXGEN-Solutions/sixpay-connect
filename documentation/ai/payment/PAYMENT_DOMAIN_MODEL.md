# SIXPAY CONNECT — Payment Domain Model

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current sub-lot:** `2.2 — Identifiers and Value Objects`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `VALUE_OBJECT_MODEL_PREPARED`  
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
| Posting reference | `PAYMENT_BANK_POSTING_REFERENCE_DECISION.md` |
| Snapshots/evidence | Lot 2.3 pending |
| Invariants | Lot 2.4 pending |
| Commands/operations | Lot 2.5 pending |
| Domain Events | Lot 2.6 pending |
| Policies/Domain Services | Lot 2.7 pending |
| Final validation | Lot 2.8 pending |

## 2. Current aggregate composition

```text
Payment
├── PaymentId
├── PaymentSource
├── ExternalPaymentReference
├── ExternalSubscriptionReference
├── PublicPaymentReference
├── PaymentRequestIdentity
│   ├── IdempotencyKey
│   ├── RequestFingerprint
│   └── CorrelationId
├── FinancialInstitutionCode
├── DebtorAccountReference
├── Money
├── TreasuryAllocationIntent
├── PaymentStatus
├── TreasuryAccountReference?
├── BankPostingReference?
├── PaymentFailure?
├── lifecycle snapshots?          [Lot 2.3]
├── timestamps
└── businessVersion
```

## 3. Key decisions

- internal identity uses UUID v4;
- public reference uses `PAY-` + ULID;
- external reference is source-scoped and case-sensitive;
- account clear values never enter Payment;
- account token, masked display and binding fingerprint are distinct;
- Treasury account comes only from protected bank configuration;
- allocation amounts use shared `Money`, same currency and exact total;
- posting command identity differs from bank posting identity;
- PaymentFailure is structured and current-only.

## 4. Code-generation status

The model is still documentary. Snapshot, invariant, command and event details
must be completed before generation.

```text
CODE GENERATION: FORBIDDEN
```
