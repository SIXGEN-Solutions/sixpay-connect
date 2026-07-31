# SIXPAY CONNECT — Payment Domain Generation Brief

> This brief is the primary human-readable context for Gate
> `IA-1 — PAYMENT DOMAIN BRIEF`.
>
> Lot 0 freezes the governing corpus. Lot 1 fixes the ubiquitous language and
> domain boundaries. Neither lot authorizes code, database, contract, client,
> server-stub or infrastructure generation.

## 1. Identification

| Property | Value |
| --- | --- |
| Domain | `payment` |
| Gate | `IA-1 — PAYMENT DOMAIN BRIEF` |
| Current lot | `1 — Ubiquitous Language and Domain Boundaries` |
| Branch | `feat/payment-domain-generation-brief` |
| Baseline date | `2026-07-31` |
| Status | `LOT_1_DRAFT_PENDING_VALIDATION` |
| Code generation | **FORBIDDEN** |
| Lot 0 authority | `documentation/ai/payment/PAYMENT_IA1_BASELINE.md` |
| Lot 1 glossary | `documentation/ai/payment/PAYMENT_UBIQUITOUS_LANGUAGE.md` |
| Lot 1 boundaries | `documentation/ai/payment/PAYMENT_DOMAIN_BOUNDARIES.md` |
| Lot 1 posting-reference decision | `documentation/ai/payment/PAYMENT_BANK_POSTING_REFERENCE_DECISION.md` |
| Next lot | `Lot 2 — Value Objects and Identifiers` |

## 2. Usage and authority

This document must be read with:

- `ENGINEERING_CONTEXT.md`;
- `documentation/ai/payment/PAYMENT_IA1_BASELINE.md`;
- `documentation/ai/payment/PAYMENT_SOURCE_BASELINE.md`;
- `documentation/ai/payment/PAYMENT_UBIQUITOUS_LANGUAGE.md`;
- `documentation/ai/payment/PAYMENT_DOMAIN_BOUNDARIES.md`;
- `documentation/ai/payment/PAYMENT_BANK_POSTING_REFERENCE_DECISION.md`;
- `documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml`;
- `documentation/contracts/CONTRACT_REGISTRY.yaml`.

The complete authority hierarchy, contract classification, approved
assumptions, file boundaries and open decisions are defined in
`PAYMENT_IA1_BASELINE.md`.

Every model rule introduced after Lot 0 must cite an approved source, a frozen
baseline decision, a Lot 1 boundary invariant or an explicit open decision.
Implementation convenience is never a business source.

## 3. Lot 0 frozen scope

IA-1 is authorized to define the Payment Aggregate Root, typed identifiers,
external references, `Money`, debtor and Treasury account references, bank
posting reference, Payment failures, Payment statuses, named transitions,
invariants, minimized decision snapshots, domain events and test traceability.

IA-1 is not authorized to generate implementation, persistence, migrations,
OpenAPI contracts, adapters, controllers, schedulers or infrastructure.

## 4. Systems of record

| Fact or operation | System of record |
| --- | --- |
| Subscription | TRESOR PAY |
| Payment order | TRESOR PAY |
| Banking customer and debtor account | Amplitude |
| Banking posting and posting lookup | Amplitude |
| Processed Payment lifecycle | SIXPAY |
| ObservedCustomer projection | SIXPAY |
| TFJ confirmation | Amplitude |
| Integration audit | SIXPAY |

## 5. Module responsibilities

- `payment` owns the aggregate, decisions, lifecycle and domain events.
- `customer` interprets customer/account verification and maintains
  `ObservedCustomer`.
- `integration` owns external transport adapters and payload mapping.
- `accounting` interprets funds verification, posting, uncertain outcomes, TFJ
  and reversal.
- `notification` owns reliable immediate and final delivery.
- `reporting` owns read-only Payment, ObservedCustomer and audit projections.
- `security` and `integration` validate authorization evidence; Payment
  consumes only a canonical minimized result.

No other business module is imported into the Payment domain model.

## 6. Approved baseline decisions

The following constraints are frozen:

- persist Payment before any Amplitude call;
- no local Subscription aggregate;
- `ExternalSubscriptionReference` is traceability only;
- validate a short-lived asymmetric signed authorization token locally;
- never persist or publish credentials or raw authorization tokens;
- only a positive canonical banking result permits progression;
- preserve a dedicated pre-posting verification boundary;
- target atomic debtor debit and configured CUT credit;
- resolve unknown write outcomes through authoritative lookup only;
- never blindly resubmit a financial command;
- distinguish immediate `CUT_CREDITED` from TFJ finality;
- only a uniquely matched successful TFJ result establishes
  `TREASURY_INTEGRATED`;
- notification delivery failure never changes financial state;
- Payment transition, immutable audit and Outbox intent are atomic;
- reuse shared-kernel `Money`;
- keep the domain framework-free;
- use named aggregate transitions, never arbitrary status mutation.

The normative identifiers and full wording are in
`PAYMENT_IA1_BASELINE.md`, section 9.

## 7. Applicable contract classification

### Active MVP, pending approval

- `tresorpay-payment-request-api-v1.yaml`
- `amplitude-payment-posting-api-v1.yaml`
- `tresorpay-payment-status-webhook-v1.yaml`
- `amplitude-end-of-day-confirmation-api-v1.yaml`
- `tresorpay-treasury-integration-webhook-v1.yaml`

### Reference MVP only

- `amplitude-customer-verification-api-v1.yaml`

### Deferred and excluded

- `tresorpay-authorization-request-api-v1.yaml`
- `tresorpay-authorization-decision-webhook-v1.yaml`

### Intentionally absent

- `tresorpay-subscription-verification-api-v1.yaml`

### Missing or not approved

- `amplitude-payment-verification-api-v1.yaml`
- internal Payment query contract;
- internal ObservedCustomer query contract;
- internal Payment audit query contract;
- external approvals listed in the Contract Registry.

No contract listed above authorizes code generation.

## 8. Golden Partner conventions

Payment will reuse these Golden Partner conventions:

- rich aggregate;
- hexagonal internal architecture;
- framework-free domain;
- application ports;
- explicit persistence mapping;
- transactional aggregate, audit and Outbox;
- append-only history;
- local domain Outbox;
- no cross-domain business imports;
- module auto-configuration;
- architecture, domain, application, persistence, concurrency and acceptance
  tests.

Partner statuses, events, thresholds, payloads and business rules are not
Payment sources.

## 9. Lot 1 ubiquitous language

The normative glossary is:

`documentation/ai/payment/PAYMENT_UBIQUITOUS_LANGUAGE.md`

The following terms have one canonical meaning:

- Payment;
- Payment received;
- TRESOR PAY authorization;
- Banking verification;
- Funds control;
- Bank posting;
- Debit;
- CUT credit;
- TFJ finality;
- Notification;
- Replay;
- Recovery;
- Uncertain banking outcome;
- Reversal;
- Business failure;
- Technical failure;
- Definitive rejection.

The TRESOR PAY operation `InitiateDebit` is normalized as submission of a
Payment order. Its acceptance proves only durable intake by SIXPAY. It does not
prove bank verification, debit, CUT credit or TFJ finality.

The source field `endToEndId` is interpreted as the unique
`ExternalPaymentReference`.

## 10. Lot 1 domain boundary

The normative ownership document is:

`documentation/ai/payment/PAYMENT_DOMAIN_BOUNDARIES.md`

Payment owns:

- lifecycle;
- identities and references;
- amount and currency;
- business decisions and normalized results;
- legal transitions;
- minimized immutable evidence;
- failures;
- business version;
- domain events;
- notification intentions.

Payment does not own:

- TRESOR PAY Subscription lifecycle;
- JWT, Subscription Key, credentials or JWKS;
- bank customer/account master data;
- available balance;
- protected CUT configuration;
- HTTP, REST, Kafka or transport concerns;
- Amplitude technical execution;
- notification delivery attempts;
- generic Outbox infrastructure;
- unmatched TFJ inputs or quarantine workflow;
- query projections or unbounded histories;
- reversal technical execution.

Payment owns the interpretation of external facts, not the external master fact.

## 11. BankPostingReference decision

Decision:

`PAY-DEC-IA1-001`

Normative document:

`documentation/ai/payment/PAYMENT_BANK_POSTING_REFERENCE_DECISION.md`

`BankPostingReference` is a composite abstraction containing:

```text
principalPostingReference   mandatory when posting is confirmed
debitLegReference           optional
cutCreditLegReference       optional
```

The principal reference identifies the atomic posting as a whole. Optional leg
references are retained only when Amplitude supplies stable identifiers. They
never represent independent Payment operations.

## 12. Authorized files

Lot 1 may add or update only:

```text
documentation/ai/payment/PAYMENT_DOMAIN_GENERATION_BRIEF.md
documentation/ai/payment/PAYMENT_SOURCE_BASELINE.md
documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml
documentation/ai/payment/PAYMENT_UBIQUITOUS_LANGUAGE.md
documentation/ai/payment/PAYMENT_DOMAIN_BOUNDARIES.md
documentation/ai/payment/PAYMENT_BANK_POSTING_REFERENCE_DECISION.md
```

Lot 1 must not modify implementation, OpenAPI artifacts, architecture,
requirements, user stories or infrastructure.

## 13. Remaining open decisions

The open-decision catalogue remains in
`PAYMENT_IA1_BASELINE.md`, section 12.

Lot 1 closes the structural form of `BankPostingReference`, but external
approval is still required for:

- Amplitude principal posting-reference format and uniqueness scope;
- optional debit/CUT leg-reference availability;
- authoritative posting lookup identifier;
- authoritative TFJ matching identifier;
- reservation capability;
- reversal guards;
- exact role of `NOTIFIED`;
- exact role of `DEBITED` under atomic posting;
- terminal `FAILED` semantics;
- security and operational parameters.

No open decision may be silently closed by generation.

## 14. Traceability rule

Every aggregate property, value object, state, transition, guard, invariant,
failure, command, event and test must cite at least one of:

```text
PAY-BASE-*
PAY-BOUND-*
PAY-POSTREF-*
PAY-DEC-*
PAY-CONTRACT-*
PAY-AI-*
PAY-SRC-*
SRC-*
OPEN-*
```

An untraced rule is invalid.

## 15. Lot 1 exit criterion

Lot 1 is complete when:

- every major Payment term has exactly one meaning;
- request acceptance, bank posting and TFJ finality are distinct;
- replay, recovery and financial resubmission are distinct;
- business failure, technical failure and uncertain outcome are distinct;
- every domain concept has one documented owner;
- Payment external evidence is minimized and canonical;
- `BankPostingReference` has one agreed structural model;
- no unresolved external detail is silently invented;
- code generation remains forbidden.

## 16. Verdict

```text
IA-1 LOT 1 UBIQUITOUS LANGUAGE AND DOMAIN BOUNDARIES PREPARED
STATUS: DRAFT_PENDING_VALIDATION
NEXT: LOT 2 — VALUE OBJECTS AND IDENTIFIERS
CODE GENERATION: FORBIDDEN
```
