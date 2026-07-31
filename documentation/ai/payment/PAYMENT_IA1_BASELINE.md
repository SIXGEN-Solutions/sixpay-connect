# SIXPAY CONNECT — IA-1 Payment Baseline and Gate Scope

> **Purpose**
>
> This document freezes the documentary corpus governing the `Payment` domain
> before any IA-1 domain modelling decision is introduced.
>
> It does not authorize application-code, persistence-schema, migration,
> OpenAPI, client, server-stub or infrastructure generation.

---

## 1. Identification

| Property | Value |
| --- | --- |
| Project | `SIXPAY CONNECT` |
| Domain | `payment` |
| Gate | `IA-1 — PAYMENT DOMAIN BRIEF` |
| Lot | `0 — Baseline and Gate Scope` |
| Repository | `SIXGEN-Solutions/sixpay-connect` |
| Authoritative branch | `feat/payment-contract-pack` |
| Frozen source commit | `__PAYMENT_CONTRACT_PACK_HEAD_SHA__` |
| Baseline date | `2026-07-31` |
| Status | `BASELINE_PENDING_VALIDATION` |
| Code generation | **FORBIDDEN** |
| Next lot | `Lot 1 — Ubiquitous Language and Payment Model Boundaries` |

The placeholder `__PAYMENT_CONTRACT_PACK_HEAD_SHA__` must be replaced with the
actual HEAD SHA of `feat/payment-contract-pack` immediately before committing
this baseline. All files in this Lot 0 patch must contain the same SHA.

---

## 2. Objective and exit rule

The objective of Lot 0 is to freeze the exact corpus that governs the future
`Payment` model.

Lot 0 is complete only when every future Payment rule can be:

1. linked to a precise authority source;
2. linked to an already approved SIXPAY decision;
3. or identified as an explicit open decision.

No domain rule may be inferred from implementation convenience, a framework,
a database mapping, an old superseded requirement, or an unapproved external
capability.

---

## 3. Authority model

### 3.1 Repository-level precedence

The repository precedence defined by `ENGINEERING_CONTEXT.md` applies:

1. current implementation on `feat/payment-contract-pack`;
2. `documentation/architecture/`;
3. `documentation/requirements/`;
4. `documentation/contracts/`;
5. `documentation/ai/`;
6. engineering assets;
7. `ENGINEERING_CONTEXT.md`.

For Payment business modelling, this general precedence is refined by the
domain-specific rules below.

### 3.2 Payment-specific precedence

| Level | Authority | Usage |
| --- | --- | --- |
| `PAY-A1` | Approved IA-0R blocking decisions | Scope and system-of-record decisions; overrides historic Customer and Subscription descriptions |
| `PAY-A2` | Approved or classified Payment contracts and `CONTRACT_REGISTRY.yaml` | Interface semantics, capability classification and external responsibility |
| `PAY-A3` | Payment IA decisions produced during IA-0P and IA-0.5P | Consolidated Payment-specific decisions and generation constraints |
| `PAY-A4` | Requirements and interoperability specifications | Business need, subject to `PAY-A1` to `PAY-A3` |
| `PAY-A5` | Architecture and engineering contracts | Structural implementation constraints, not new business rules |
| `PAY-A6` | Golden Partner implementation | Reusable coding and module conventions only |
| `PAY-A7` | Existing `backend/payment` implementation | Current technical state only; never a source of new business rules |

### 3.3 Arbitration rules

1. A contract location does not imply that the contract is active.
2. `CONTRACT_REGISTRY.yaml` and each contract classification govern lifecycle.
3. A `DEFERRED_FUTURE` or `EXCLUDED` capability cannot influence the MVP model.
4. A `REFERENCE_MVP` contract may inform the model only within its explicit
   scope and limitations.
5. A `PENDING_APPROVAL` contract may define a candidate boundary but cannot
   authorize code generation.
6. An IA-0R decision overrides older requirements that assign subscription
   ownership or customer-master authority to SIXPAY.
7. Architecture defines structure and boundaries; it cannot invent a Payment
   status, invariant, event or financial effect.
8. Golden Partner defines implementation conventions; it is not a source of
   Payment business semantics.
9. Any unresolved contradiction becomes an `OPEN-*` decision.
10. No rule is silently selected by an AI coding assistant.

---

## 4. Authoritative Payment AI corpus

The following files form the approved reading corpus for IA-1.

| Source ID | Path | Role | IA-1 usage |
| --- | --- | --- | --- |
| `PAY-AI-001` | `documentation/ai/payment/PAYMENT_SOURCE_BASELINE.md` | Historic and normalized source catalogue | Normative after Lot 0 normalization |
| `PAY-AI-002` | `documentation/ai/payment/GATE_IA_0P_PAYMENT_PREFLIGHT.md` | IA-0P scope and verdict | Normative for MVP boundary |
| `PAY-AI-003` | `documentation/ai/payment/PAYMENT_CONTEXT_MAP.md` | Systems and module responsibilities | Normative for ownership boundaries |
| `PAY-AI-004` | `documentation/ai/payment/PAYMENT_BUSINESS_FLOWS.md` | Nominal and alternative flows | Normative where consistent with approved contracts |
| `PAY-AI-005` | `documentation/ai/payment/PAYMENT_DOMAIN_MODEL.md` | Preflight model candidates | Input to IA-1, not automatically final |
| `PAY-AI-006` | `documentation/ai/payment/PAYMENT_STATE_MACHINE.yaml` | Existing lifecycle baseline | Input to IA-1; changes require explicit decision |
| `PAY-AI-007` | `documentation/ai/payment/PAYMENT_EVENT_CATALOG.yaml` | Existing event baseline | Input to IA-1; events require source traceability |
| `PAY-AI-008` | `documentation/ai/payment/PAYMENT_CONTRACT_REQUIREMENTS.yaml` | Contract requirements | Normative for interface coverage |
| `PAY-AI-009` | `documentation/ai/payment/PAYMENT_SECURITY_AUDIT_BASELINE.md` | Security, audit and observability | Normative |
| `PAY-AI-010` | `documentation/ai/payment/PAYMENT_RESILIENCE_BASELINE.md` | Retry, replay and recovery | Normative |
| `PAY-AI-011` | `documentation/ai/payment/PAYMENT_ACCEPTANCE_SCENARIOS.md` | Acceptance scenarios | Verification source |
| `PAY-AI-012` | `documentation/ai/payment/PAYMENT_AUTHORIZATION_VERIFICATION_DECISION.md` | TRESOR PAY authorization decision | Decided, pending external approval |
| `PAY-AI-013` | `documentation/ai/payment/AMPLITUDE_CUSTOMER_VERIFICATION_PAYMENT_REVIEW.md` | Customer/account verification scope | Confirmed reference scope |
| `PAY-AI-014` | `documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml` | Machine-readable context | Must remain synchronized |
| `PAY-AI-015` | `documentation/ai/payment/PAYMENT_DOMAIN_GENERATION_BRIEF.md` | Human-readable IA-1 brief | Primary IA-1 document |
| `PAY-AI-016` | `documentation/ai/payment/PAYMENT_IA1_BASELINE.md` | Lot 0 frozen baseline | Primary Lot 0 authority |

No source outside this corpus may introduce a new IA-1 Payment business rule
without being registered through a documented decision.

---

## 5. Applicable contract inventory

### 5.1 Active MVP capability, pending approval

These contracts are applicable to the MVP capability map but remain
`PENDING_APPROVAL`, `REFERENCE_ONLY`, and forbidden for code generation.

| Contract ID | Path | Capability |
| --- | --- | --- |
| `PAY-CONTRACT-001` | `documentation/contracts/tresorpay/tresorpay-payment-request-api-v1.yaml` | Receive and durably accept a TRESOR PAY payment request |
| `PAY-CONTRACT-002` | `documentation/contracts/amplitude/amplitude-payment-posting-api-v1.yaml` | Execution-time checks, posting, authoritative lookup and reversal |
| `PAY-CONTRACT-003` | `documentation/contracts/tresorpay/tresorpay-payment-status-webhook-v1.yaml` | Immediate Payment lifecycle notification |
| `PAY-CONTRACT-004` | `documentation/contracts/amplitude/amplitude-end-of-day-confirmation-api-v1.yaml` | TFJ confirmation and reconciliation lookup |
| `PAY-CONTRACT-005` | `documentation/contracts/tresorpay/tresorpay-treasury-integration-webhook-v1.yaml` | Final Treasury integration notification |

### 5.2 Reference MVP only

| Contract ID | Path | Authorized usage | Explicit limitations |
| --- | --- | --- | --- |
| `PAY-CONTRACT-006` | `documentation/contracts/amplitude/amplitude-customer-verification-api-v1.yaml` | Customer, NIU, account, ownership, status, debit block, opposition and required KYC facts | No subscription validation, funds control, posting, reversal or TFJ |

Only a canonical `VERIFIED` result may allow Payment to progress. Raw Amplitude
payloads must remain outside the aggregate and domain events.

### 5.3 Intentionally absent

| Contract ID | Expected path | Decision |
| --- | --- | --- |
| `PAY-CONTRACT-007` | `documentation/contracts/tresorpay/tresorpay-subscription-verification-api-v1.yaml` | Not required for the MVP because authorization is a locally validated short-lived signed token |

Its absence is not a missing deliverable while
`LOCAL_SIGNED_TOKEN_VALIDATION` remains the approved MVP decision.

### 5.4 Deferred and excluded

| Contract ID | Path | Classification |
| --- | --- | --- |
| `PAY-CONTRACT-008` | `documentation/contracts/tresorpay/tresorpay-authorization-request-api-v1.yaml` | `DEFERRED_FUTURE`, `DRAFT`, `EXCLUDED` |
| `PAY-CONTRACT-009` | `documentation/contracts/tresorpay/tresorpay-authorization-decision-webhook-v1.yaml` | `DEFERRED_FUTURE`, `DRAFT`, `EXCLUDED` |

These contracts are style references only. They cannot introduce local
Subscription lifecycle concepts into Payment.

### 5.5 Missing or not yet approved

| Contract ID | Expected path | Capability | Effect on IA-1 |
| --- | --- | --- | --- |
| `PAY-CONTRACT-MISSING-001` | `documentation/contracts/amplitude/amplitude-payment-verification-api-v1.yaml` | Dedicated pre-posting funds, restriction and payment-limit verification | Model must preserve the boundary; exact payload fields remain open |
| `PAY-CONTRACT-MISSING-002` | `documentation/contracts/internal/payment-query-api-v1.yaml` or registry-approved equivalent | Payment query | Write aggregate must not be shaped by an unapproved query payload |
| `PAY-CONTRACT-MISSING-003` | `documentation/contracts/internal/observed-customer-query-api-v1.yaml` or registry-approved equivalent | ObservedCustomer query | Remains outside Payment aggregate |
| `PAY-CONTRACT-MISSING-004` | `documentation/contracts/internal/payment-audit-query-api-v1.yaml` or registry-approved equivalent | Payment audit query | Audit projection remains separate from aggregate state |

The exact paths of internal query contracts must follow the authoritative
repository structure and registry. No duplicate `sixpay-*` naming is introduced
by IA-1 without approval.

---

## 6. Capability classification

### 6.1 Active for the MVP

| Capability ID | Capability | Owner |
| --- | --- | --- |
| `MVP-PAY-001` | Receive an authenticated TRESOR PAY payment order | `integration` + `payment` |
| `MVP-PAY-002` | Persist Payment before any Amplitude call | `payment` |
| `MVP-PAY-003` | Validate signed authorization evidence locally | `security` + `integration`; Payment consumes canonical result |
| `MVP-PAY-004` | Verify banking customer and debtor account | `customer` + `integration`; Payment decides progression |
| `MVP-PAY-005` | Verify account state, restrictions and opposition | `customer` + `integration`; Payment decides progression |
| `MVP-PAY-006` | Verify funds and execution constraints | `accounting` + `integration`; Payment consumes decision |
| `MVP-PAY-007` | Debit the debtor account and credit configured CUT | `accounting` + `integration`; Payment owns lifecycle |
| `MVP-PAY-008` | Recover uncertain posting through authoritative lookup | `accounting` + `integration` |
| `MVP-PAY-009` | Notify the immediate result reliably | `notification` |
| `MVP-PAY-010` | Await and reconcile TFJ confirmation | `accounting` + `payment` |
| `MVP-PAY-011` | Notify the final Treasury result | `notification` |
| `MVP-PAY-012` | Expose query and immutable audit projections | `reporting` |
| `MVP-PAY-013` | Support authorized reversal | `accounting` + `payment` |
| `MVP-PAY-014` | Enforce idempotency, correlation, audit and Outbox | Cross-cutting, transactionally anchored in `payment` |

### 6.2 Reference only

- Customer Verification transport semantics outside their explicit Payment use.
- Deferred TRESOR PAY authorization contracts as style references.
- Golden Partner package layout, rich aggregate, ports/adapters, audit, Outbox,
  architecture tests and acceptance traceability.
- Existing IA-0P model names where IA-1 has not yet confirmed their final form.
- Historical SLA values and technology-specific worker descriptions.

### 6.3 Deferred

- local Subscription management;
- SIXPAY-assisted subscription authorization;
- digital KYC using document and selfie;
- interbank clearing beyond TFJ confirmation;
- TRESOR PAY merchant management;
- SFTP and Sandbox payment scenarios;
- mobile applications;
- migration to distributed microservices or mandatory Kafka transport;
- any new real-time TRESOR PAY subscription verification API.

### 6.4 Missing or not approved

- exact pre-posting verification contract;
- final external approval of Payment Contract Pack;
- final bank confirmation of reservation capability;
- final bank confirmation of atomic posting and partial-outcome semantics;
- final bank confirmation of reversal capability;
- final TRESOR PAY approval of signed-token claims, JWKS and webhook semantics;
- final Security approval of token TTL, clock skew and cryptographic profiles;
- final Operations approval of cutoff, quarantine, retry, DLQ and replay runbooks;
- final internal query contracts and approval;
- exact HEAD SHA replacement in all Lot 0 files.

---

## 7. Included IA-1 scope

IA-1 may define, with source traceability:

- the `Payment` Aggregate Root;
- `PaymentId`;
- `ExternalPaymentReference`;
- `ExternalSubscriptionReference`;
- `Money`, by reusing the shared-kernel concept;
- `DebtorAccountReference`;
- `TreasuryAccountReference`;
- `BankPostingReference`;
- `PaymentFailure`;
- `PaymentStatus`;
- aggregate commands and named transitions;
- aggregate invariants;
- canonical minimized decision snapshots;
- domain events raised by aggregate decisions;
- repository and port responsibilities at conceptual level;
- testable acceptance rules;
- confidentiality classifications;
- traceability from source to rule and test.

IA-1 may refine an existing IA-0P candidate only through an explicit,
traceable decision.

---

## 8. Out of scope for IA-1

IA-1 must not:

- generate or modify Java implementation;
- create JPA entities or mappings;
- create database tables, indexes or Flyway migrations;
- create or modify OpenAPI contracts;
- create controllers, clients, adapters or schedulers;
- select retry numbers, timeouts, SLAs or cutoff times not approved by owners;
- model a local Subscription aggregate;
- embed `ObservedCustomer` in the Payment aggregate;
- make Payment own raw banking customer or account master data;
- make notification delivery status determine financial status;
- make `CUT_CREDITED` equivalent to TFJ finality;
- make a timeout equivalent to posting failure;
- authorize blind financial replay;
- authorize automatic reversal from TFJ delay alone;
- persist or publish raw credentials, authorization tokens, full accounts,
  secrets, PINs or unnecessary KYC data;
- introduce a new architecture, package root or module;
- copy Partner business concepts into Payment;
- use deferred contracts to generate MVP behavior.

---

## 9. Approved assumptions and decisions

The following decisions are frozen inputs for IA-1.

| Decision ID | Decision |
| --- | --- |
| `PAY-BASE-001` | `Payment` is the sole write Aggregate Root of the Payment domain scope. |
| `PAY-BASE-002` | SIXPAY is the system of record for the processed Payment and its integration audit. |
| `PAY-BASE-003` | TRESOR PAY remains the system of record for the payment order and subscription. |
| `PAY-BASE-004` | Amplitude remains the system of record for banking customer, account, posting and TFJ result. |
| `PAY-BASE-005` | The Payment request is durably persisted before any Amplitude call. |
| `PAY-BASE-006` | No local Subscription aggregate, validation lifecycle or subscription status is created. |
| `PAY-BASE-007` | `ExternalSubscriptionReference` is traceability data only. |
| `PAY-BASE-008` | MVP subscription authorization uses a short-lived asymmetric signed JWT/JWS validated locally. |
| `PAY-BASE-009` | Token, Subscription Key and credentials are never aggregate data and are never logged or published. |
| `PAY-BASE-010` | Only a positive canonical banking decision allows progression. |
| `PAY-BASE-011` | Customer Verification does not cover funds control or posting. |
| `PAY-BASE-012` | A dedicated pre-posting verification boundary is preserved even while its contract is missing. |
| `PAY-BASE-013` | Posting targets an atomic debtor debit and configured CUT credit when approved by Amplitude. |
| `PAY-BASE-014` | Treasury account reference comes from protected bank-controlled configuration, not freely from TRESOR PAY. |
| `PAY-BASE-015` | A write timeout or ambiguous response does not prove failure. |
| `PAY-BASE-016` | Unknown posting outcomes permit authoritative lookup only, never blind resubmission. |
| `PAY-BASE-017` | A stable bank posting reference is retained when available. |
| `PAY-BASE-018` | `CUT_CREDITED` is an immediate posting fact and does not establish TFJ finality. |
| `PAY-BASE-019` | Only a durably persisted, uniquely matched successful Amplitude TFJ result establishes `TREASURY_INTEGRATED`. |
| `PAY-BASE-020` | Unmatched, ambiguous or conflicting TFJ results are quarantined and do not change Payment. |
| `PAY-BASE-021` | Missing TFJ confirmation keeps Payment pending and triggers reconciliation; delay alone does not authorize reversal. |
| `PAY-BASE-022` | Notification delivery failure never changes Payment financial state. |
| `PAY-BASE-023` | Notification replay preserves business event identity and never triggers financial replay. |
| `PAY-BASE-024` | Payment transition, immutable audit trace and Outbox intent are transactionally atomic. |
| `PAY-BASE-025` | Audit and history are append-only. |
| `PAY-BASE-026` | `Money` must reuse the shared-kernel value object unless IA-1 documents an incompatibility and obtains approval. |
| `PAY-BASE-027` | The domain layer remains framework-free. |
| `PAY-BASE-028` | Controllers, JPA entities and external transport payloads remain outside the domain model. |
| `PAY-BASE-029` | Payment methods represent named business transitions; no arbitrary status setter is permitted. |
| `PAY-BASE-030` | Every mutation must have acceptance traceability and automated proof at the appropriate level. |

---

## 10. Golden Partner conventions

The Golden Partner module is authoritative for implementation conventions only.

### 10.1 Conventions to reproduce

- hexagonal internal architecture;
- rich aggregate with named transitions;
- framework-free domain;
- application services depending on ports;
- explicit domain-to-JPA mapping;
- transactional aggregate, audit and Outbox writes;
- append-only history;
- local domain Outbox;
- no business-domain-to-business-domain imports;
- top-level `configuration` package for module assembly;
- Spring Boot auto-configuration for a non-executable module JAR;
- reuse of `common`, `shared-kernel`, `security` and validated `integration`
  contracts;
- architecture tests;
- domain tests;
- application tests;
- persistence and concurrency integration tests;
- API contract tests when contracts are approved;
- acceptance traceability matrix.

### 10.2 Conventions not to copy as business rules

- Partner statuses;
- Partner events;
- Partner validation thresholds;
- Partner API payloads;
- Partner authorization perimeter;
- Partner notification content;
- Partner database table names;
- Partner idempotency semantics where Payment contracts define stricter rules.

---

## 11. Authorized and forbidden files

### 11.1 Authorized reading sources

```text
ENGINEERING_CONTEXT.md
AI_GENERATION_STRATEGY.md
DOMAIN_GENERATION_BRIEF_TEMPLATE.md
AI_CONTEXT_MANIFEST_TEMPLATE.yaml
backend/SIXPAY_BACKEND_ENGINEERING_GENERATION_CONTRACT.md
backend/SIXPAY_BACKEND_TECHNOLOGY_MATRIX.md
backend/common/**
backend/shared-kernel/**
backend/security/**
backend/integration/**
backend/partner/**
backend/payment/**
documentation/architecture/**
documentation/requirements/**
documentation/contracts/**
documentation/ai/customer/**
documentation/ai/payment/**
```

Reading authorization does not make every statement normative. The precedence
and classification rules in this baseline remain mandatory.

### 11.2 Files authorized for Lot 0 modification

```text
documentation/ai/payment/PAYMENT_DOMAIN_GENERATION_BRIEF.md
documentation/ai/payment/PAYMENT_IA1_BASELINE.md
documentation/ai/payment/PAYMENT_SOURCE_BASELINE.md
documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml
documentation/contracts/CONTRACT_REGISTRY.yaml
```

Changes to `CONTRACT_REGISTRY.yaml` are restricted to metadata normalization,
missing-contract inventory and path consistency. Lot 0 must not change contract
business semantics, approval status or generation authorization.

### 11.3 Files forbidden for Lot 0 modification

```text
backend/**
frontend/**
infrastructure/**
deployment/**
scripts/**
.github/**
documentation/architecture/**
documentation/requirements/**
documentation/ai/customer/**
documentation/contracts/amplitude/*.yaml
documentation/contracts/tresorpay/*.yaml
documentation/contracts/internal/*.yaml
```

---

## 12. Open decisions

| Open ID | Decision | Impact | Required owner |
| --- | --- | --- | --- |
| `OPEN-BASE-001` | Confirm and replace the actual HEAD SHA of `feat/payment-contract-pack`. | Baseline reproducibility | Repository maintainer |
| `OPEN-BASE-002` | Approve the complete Payment Contract Pack for generation. | Code generation remains forbidden | Contract owners |
| `OPEN-BASE-003` | Produce and approve `amplitude-payment-verification-api-v1.yaml`. | Exact pre-posting decision fields remain unavailable | Amplitude, Accounting, Payment, Integration |
| `OPEN-BASE-004` | Confirm final internal query contract paths and names. | Query APIs remain outside aggregate design | Reporting, Payment, Architecture |
| `OPEN-BASE-005` | Resolve the historical eight-versus-nine contract artifact count. | Manifest consistency | Architecture and Integration |
| `OPEN-BASE-006` | Confirm whether reservation is supported and required. | Aggregate may track reservation only if approved | Amplitude and Accounting |
| `OPEN-BASE-007` | Confirm exact bank posting reference structure and uniqueness. | `BankPostingReference` final shape | Amplitude and Accounting |
| `OPEN-BASE-008` | Confirm approved reversal source states and authorization evidence. | Reversal transition guards | Accounting, Operations, Payment |
| `OPEN-BASE-009` | Confirm the final semantic role of `NOTIFIED`. | Financial state machine versus delivery projection | Payment, Notification, Architecture |
| `OPEN-BASE-010` | Confirm whether `DEBITED` can exist independently when preferred posting is atomic. | State-machine precision | Amplitude, Accounting, Payment |
| `OPEN-BASE-011` | Confirm exact use of terminal `FAILED`. | Failure taxonomy and allowed transitions | Payment and Operations |
| `OPEN-BASE-012` | Confirm final JWT TTL, clock skew and accepted algorithms. | Security adapter, not aggregate shape | Security and TRESOR PAY |
| `OPEN-BASE-013` | Confirm cutoff, reconciliation windows and operational SLA. | Scheduling and alerts, not aggregate invariants | Operations and Accounting |

No open decision may be silently closed during Lot 1.

---

## 13. Traceability rule for future IA-1 content

Every future Payment element must include at least one traceability identifier:

- `PAY-BASE-*` for an approved Lot 0 decision;
- `PAY-CONTRACT-*` for a contract;
- `PAY-AI-*` for an IA source;
- `PAY-SRC-*` or `SRC-*` for an existing source catalogue entry;
- `OPEN-*` for a decision not yet approved.

This applies to:

- aggregate fields;
- value objects;
- statuses;
- transitions;
- guards;
- invariants;
- failure codes;
- commands;
- domain events;
- repository operations;
- confidentiality rules;
- test scenarios.

A rule with no traceability identifier is invalid.

---

## 14. Lot 0 validation checklist

- [ ] Actual branch HEAD SHA replaces every placeholder.
- [ ] The same SHA appears in all updated Lot 0 files.
- [ ] Contract registry branch metadata is normalized.
- [ ] Active, reference, deferred, intentional-absence and missing contracts are distinguished.
- [ ] No approval status is upgraded by Lot 0.
- [ ] No code-generation lock is removed.
- [ ] Included and excluded IA-1 scope is explicit.
- [ ] Golden Partner business rules are not imported.
- [ ] Every approved assumption has an identifier.
- [ ] Every unresolved issue has an `OPEN-*` identifier.
- [ ] `AI_CONTEXT_MANIFEST.yaml` points to this baseline.
- [ ] YAML files pass parsing and repository linting.
- [ ] Human review validates the documentary baseline.

---

## 15. Verdict

Until the validation checklist is complete:

```text
IA-1 LOT 0 BASELINE PREPARED
STATUS: BASELINE_PENDING_VALIDATION
CODE GENERATION: FORBIDDEN
```

After documentary approval:

```text
IA-1 LOT 0 BASELINE APPROVED
READY FOR LOT 1 — UBIQUITOUS LANGUAGE AND PAYMENT MODEL BOUNDARIES
CODE GENERATION: FORBIDDEN
```
