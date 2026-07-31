# SIXPAY CONNECT — Payment Domain Model

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current sub-lot:** `2.1 — Aggregate Root Payment`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `AGGREGATE_ROOT_MODEL_PREPARED`  
> **Code generation:** **FORBIDDEN**

## 1. Purpose

This document is the consolidated IA-1 domain-model index.

The former IA-0P model was a candidate model. IA-1 now progressively replaces
it through validated sub-lots.

For Lot 2.1, the normative Aggregate Root specification is:

`documentation/ai/payment/PAYMENT_AGGREGATE_ROOT.md`

## 2. Normative model hierarchy

| Topic | Normative document |
| --- | --- |
| Sources and authority | `PAYMENT_SOURCE_BASELINE.md` |
| Gate scope and decisions | `PAYMENT_IA1_BASELINE.md` |
| Ubiquitous language | `PAYMENT_UBIQUITOUS_LANGUAGE.md` |
| Domain ownership | `PAYMENT_DOMAIN_BOUNDARIES.md` |
| Aggregate Root | `PAYMENT_AGGREGATE_ROOT.md` |
| Bank posting reference | `PAYMENT_BANK_POSTING_REFERENCE_DECISION.md` |
| Identifiers and Value Objects | Lot 2.2 — pending |
| Snapshots and evidence | Lot 2.3 — pending |
| Invariants | Lot 2.4 — pending |
| Commands and operations | Lot 2.5 — pending |
| Domain Events | Lot 2.6 — pending |
| Policies and Domain Services | Lot 2.7 — pending |
| Final model validation | Lot 2.8 — pending |

## 3. Aggregate model

`Payment` is the sole write Aggregate Root.

```text
Payment
├── immutable identity and original intent
│   ├── PaymentId
│   ├── PaymentSource
│   ├── ExternalPaymentReference
│   ├── ExternalSubscriptionReference
│   ├── PublicPaymentReference
│   ├── PaymentRequestIdentity
│   ├── FinancialInstitutionCode
│   ├── DebtorAccountReference
│   ├── Money
│   └── Treasury allocation intent
│
├── current decision state
│   ├── PaymentStatus
│   ├── current relevant PaymentFailure?
│   ├── AuthorizationEvidenceSnapshot?
│   ├── BankingVerificationSnapshot?
│   ├── FundsControlSnapshot?
│   ├── TreasuryAccountReference?
│   ├── PostingOutcomeSnapshot?
│   ├── BankPostingReference?
│   ├── EndOfDayConfirmationSnapshot?
│   └── ReversalSnapshot?
│
└── consistency metadata
    ├── createdAt
    ├── updatedAt
    ├── finalizedAt?
    └── businessVersion
```

The exact type details remain deferred to Lots 2.2 and 2.3.

## 4. Aggregate boundary

Payment owns lifecycle decisions and minimized evidence.

It does not own Subscription, customer/account master data, bank configuration,
external transports, notification delivery, generic Outbox infrastructure,
unmatched TFJ workflow, reversal execution or read projections.

## 5. Creation and reconstitution

Creation is a named business operation and raises a Payment-received fact.

Reconstitution restores persisted state and raises no event.

No persistence adapter may rebuild the aggregate by replaying public transition
methods.

## 6. Current decisions

- uniqueness is scoped by `PaymentSource + ExternalPaymentReference`;
- MVP source is `TRESOR_PAY`;
- aggregate retains only current relevant `PaymentFailure`;
- complete failure history belongs to audit/reporting;
- notification is a domain intent, not financial status;
- reversal stays on the original Payment;
- original posting identity remains after reversal;
- aggregate methods are named and no generic status setter is allowed.

## 7. State-machine relationship

The existing `PAYMENT_STATE_MACHINE.yaml` remains an IA-1 input, not yet a
final implementation contract.

Lot 2.1 confirms responsibility for the state machine but does not close every
state or transition. In particular, later work must reconcile:

- `NOTIFIED` with notification-as-intent;
- `DEBITED` with atomic debit + CUT credit;
- `FAILED` with business, technical and uncertain outcomes.

## 8. Generation status

```text
AGGREGATE ROOT MODEL: PREPARED
VALUE OBJECT CATALOGUE: PENDING LOT 2.2
CODE GENERATION: FORBIDDEN
```
