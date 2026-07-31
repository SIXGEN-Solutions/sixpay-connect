# SIXPAY CONNECT — Payment Domain Generation Brief

> This brief is the primary human-readable context for Gate
> `IA-1 — PAYMENT DOMAIN BRIEF`.
>
> Lot 0 freezes the governing corpus. It does not authorize code, database,
> contract, client, server-stub or infrastructure generation.

## 1. Identification

| Property | Value |
| --- | --- |
| Domain | `payment` |
| Gate | `IA-1 — PAYMENT DOMAIN BRIEF` |
| Current lot | `0 — Baseline and Gate Scope` |
| Branch | `feat/payment-contract-pack` |
| Frozen source commit | `__PAYMENT_CONTRACT_PACK_HEAD_SHA__` |
| Baseline date | `2026-07-31` |
| Status | `BASELINE_PENDING_VALIDATION` |
| Code generation | **FORBIDDEN** |
| Lot 0 authority | `documentation/ai/payment/PAYMENT_IA1_BASELINE.md` |
| Next lot | `Lot 1 — Ubiquitous Language and Payment Model Boundaries` |

## 2. Usage and authority

This document must be read with:

- `ENGINEERING_CONTEXT.md`;
- `documentation/ai/payment/PAYMENT_IA1_BASELINE.md`;
- `documentation/ai/payment/PAYMENT_SOURCE_BASELINE.md`;
- `documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml`;
- `documentation/contracts/CONTRACT_REGISTRY.yaml`.

The complete authority hierarchy, contract classification, approved
assumptions, file boundaries and open decisions are defined in
`PAYMENT_IA1_BASELINE.md`.

Every model rule introduced after Lot 0 must cite an approved source or an
explicit open decision. Implementation convenience is never a business source.

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
- `accounting` interprets funds verification, posting, unknown outcomes, TFJ
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

No contract listed above authorizes code generation at Lot 0.

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

## 9. Authorized files

Lot 0 may update only:

```text
documentation/ai/payment/PAYMENT_DOMAIN_GENERATION_BRIEF.md
documentation/ai/payment/PAYMENT_IA1_BASELINE.md
documentation/ai/payment/PAYMENT_SOURCE_BASELINE.md
documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml
documentation/contracts/CONTRACT_REGISTRY.yaml
```

Lot 0 must not modify implementation, OpenAPI artifacts, architecture,
requirements, user stories or infrastructure.

## 10. Open decisions

The open-decision catalogue is maintained in
`PAYMENT_IA1_BASELINE.md`, section 12.

The principal blockers are:

- exact branch HEAD SHA;
- Payment Contract Pack approval;
- missing pre-posting verification contract;
- final internal query contract names and paths;
- reservation capability;
- bank posting reference structure;
- reversal guards;
- semantic role of `NOTIFIED`;
- semantics of `DEBITED` under atomic posting;
- exact role of terminal `FAILED`;
- security and operational parameters.

No open decision may be silently closed by generation.

## 11. Traceability rule

Every aggregate property, value object, state, transition, guard, invariant,
failure, command, event and test must cite at least one of:

```text
PAY-BASE-*
PAY-CONTRACT-*
PAY-AI-*
PAY-SRC-*
SRC-*
OPEN-*
```

An untraced rule is invalid.

## 12. Lot 0 exit criterion

Lot 0 is complete when:

- the authoritative branch HEAD is frozen;
- the same SHA appears in all Lot 0 documents;
- the contract inventory is classified;
- active, reference, deferred, intentionally absent and missing capabilities
  are distinguished;
- approved assumptions are identified;
- unresolved decisions are explicitly catalogued;
- authorized and forbidden files are explicit;
- the baseline passes human validation;
- code generation remains forbidden.

## 13. Verdict

```text
IA-1 LOT 0 BASELINE PREPARED
STATUS: BASELINE_PENDING_VALIDATION
NEXT: DOCUMENTARY BASELINE APPROVAL
CODE GENERATION: FORBIDDEN
```
