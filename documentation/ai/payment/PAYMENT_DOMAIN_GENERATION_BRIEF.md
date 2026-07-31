# SIXPAY CONNECT — Payment Domain Generation Brief

> This brief is the primary human-readable context for Gate
> `IA-1 — PAYMENT DOMAIN BRIEF`.
>
> Lots 0 and 1 freeze the governing corpus, language and boundaries.
> Lot 2.1 defines the Payment Aggregate Root. No current lot authorizes code,
> database, contract, client, server-stub or infrastructure generation.

## 1. Identification

| Property | Value |
| --- | --- |
| Domain | `payment` |
| Gate | `IA-1 — PAYMENT DOMAIN BRIEF` |
| Current lot | `2.1 — Aggregate Root Payment` |
| Branch | `feat/payment-domain-generation-brief` |
| Baseline date | `2026-07-31` |
| Status | `LOT_2_1_DRAFT_PENDING_VALIDATION` |
| Code generation | **FORBIDDEN** |
| Lot 0 authority | `documentation/ai/payment/PAYMENT_IA1_BASELINE.md` |
| Lot 1 glossary | `documentation/ai/payment/PAYMENT_UBIQUITOUS_LANGUAGE.md` |
| Lot 1 boundaries | `documentation/ai/payment/PAYMENT_DOMAIN_BOUNDARIES.md` |
| Lot 2.1 Aggregate Root | `documentation/ai/payment/PAYMENT_AGGREGATE_ROOT.md` |
| Next lot | `Lot 2.2 — Identifiers and Value Objects` |

## 2. Usage and authority

This document must be read with:

- `ENGINEERING_CONTEXT.md`;
- `PAYMENT_IA1_BASELINE.md`;
- `PAYMENT_SOURCE_BASELINE.md`;
- `PAYMENT_UBIQUITOUS_LANGUAGE.md`;
- `PAYMENT_DOMAIN_BOUNDARIES.md`;
- `PAYMENT_BANK_POSTING_REFERENCE_DECISION.md`;
- `PAYMENT_AGGREGATE_ROOT.md`;
- `PAYMENT_DOMAIN_MODEL.md`;
- `AI_CONTEXT_MANIFEST.yaml`;
- `documentation/contracts/CONTRACT_REGISTRY.yaml`.

Every rule must cite an approved source, decision, invariant or open decision.
Implementation convenience is never a business source.

## 3. Systems of record

| Fact or operation | System of record |
| --- | --- |
| Subscription and Payment order | TRESOR PAY |
| Banking customer/account, posting and TFJ | Amplitude |
| Processed Payment lifecycle | SIXPAY |
| ObservedCustomer and query projections | SIXPAY |
| Integration audit | SIXPAY |

## 4. Module responsibilities

- `payment` owns aggregate decisions, lifecycle and domain events.
- `customer` interprets customer/account verification.
- `accounting` interprets funds, posting, uncertain outcomes, TFJ and reversal.
- `integration` owns transport adapters and external mapping.
- `notification` owns delivery.
- `reporting` owns read projections.
- `security` and `integration` validate authorization evidence.

## 5. Frozen baseline decisions

- persist Payment before Amplitude calls;
- no local Subscription aggregate;
- external subscription reference is traceability only;
- signed authorization token validated locally;
- credentials never enter Payment;
- only positive canonical decisions permit progression;
- posting targets atomic debit + configured CUT credit;
- uncertain financial outcomes are looked up, never blindly replayed;
- CUT credit is distinct from TFJ finality;
- notification delivery does not change financial state;
- state, audit and Outbox intent are atomic;
- shared-kernel `Money` is reused;
- domain remains framework-free.

## 6. Ubiquitous language and boundaries

Normative documents:

- `PAYMENT_UBIQUITOUS_LANGUAGE.md`;
- `PAYMENT_DOMAIN_BOUNDARIES.md`.

`InitiateDebit` means Payment-order submission, not confirmed debit.
`endToEndId` means `ExternalPaymentReference`.

## 7. Bank posting reference

Decision `PAY-DEC-IA1-001` defines:

```text
principalPostingReference   mandatory after confirmed posting
debitLegReference           optional
cutCreditLegReference       optional
```

## 8. Lot 2.1 Aggregate Root

Normative document:

`documentation/ai/payment/PAYMENT_AGGREGATE_ROOT.md`

### Aggregate decision

`Payment` is the sole write Aggregate Root and represents one logical TRESOR
PAY payment intention for its full lifecycle.

### Aggregate-owned concepts

Payment owns:

- immutable identity and original intent;
- lifecycle status;
- amount and currency;
- current canonical decisions;
- minimized evidence;
- current relevant failure;
- posting and reversal identity;
- temporal and business version consistency;
- domain-event registration.

### Aggregate-excluded concepts

Payment excludes:

- Subscription lifecycle;
- credentials and raw payloads;
- bank master data and configuration;
- network execution;
- notification delivery;
- generic Outbox implementation;
- unmatched TFJ workflow;
- unbounded histories and read projections.

### Creation and reconstitution

`receive` creates a new Payment and raises the received fact.

`reconstitute` restores persisted state, applies no transition and raises no
event.

### Transaction boundary

One transaction loads or creates one Payment, applies one use-case decision and
atomically persists aggregate state, immutable audit and Outbox intent.

### Decisions closed

- `PAY-DEC-IA1-002`: source-scoped external-reference uniqueness;
- `PAY-DEC-IA1-003`: current failure in aggregate, history in audit;
- `PAY-DEC-IA1-004`: notification is intent, not financial state;
- `PAY-DEC-IA1-005`: reconstitution is distinct and event-free;
- `PAY-DEC-IA1-006`: reversal remains on original Payment.

## 9. Deferred to following sub-lots

| Lot | Scope |
| --- | --- |
| 2.2 | Identifier and Value Object catalogue |
| 2.3 | Snapshots and business evidence |
| 2.4 | Complete invariants |
| 2.5 | Commands and aggregate operations |
| 2.6 | Domain Events |
| 2.7 | Policies and Domain Services |
| 2.8 | Final model validation |

## 10. Traceability prefixes

```text
PAY-BASE-*
PAY-BOUND-*
PAY-POSTREF-*
PAY-AGG-*
PAY-DEC-*
PAY-CONTRACT-*
PAY-AI-*
PAY-SRC-*
SRC-*
OPEN-*
```

An untraced rule is invalid.

## 11. Authorized Lot 2.1 files

```text
documentation/ai/payment/PAYMENT_AGGREGATE_ROOT.md
documentation/ai/payment/PAYMENT_DOMAIN_MODEL.md
documentation/ai/payment/PAYMENT_DOMAIN_GENERATION_BRIEF.md
documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml
```

No implementation, contract, architecture or requirement file is modified.

## 12. Verdict

```text
IA-1 LOT 2.1 PAYMENT AGGREGATE ROOT PREPARED
STATUS: DRAFT_PENDING_VALIDATION
NEXT: LOT 2.2 — IDENTIFIERS AND VALUE OBJECTS
CODE GENERATION: FORBIDDEN
```
