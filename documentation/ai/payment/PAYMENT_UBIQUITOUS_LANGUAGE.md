# SIXPAY CONNECT — Payment Ubiquitous Language

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Lot:** `1 — Ubiquitous Language and Domain Boundaries`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `DRAFT_PENDING_VALIDATION`  
> **Code generation:** **FORBIDDEN**

## 1. Purpose

This glossary establishes one precise meaning for every major Payment term.
A term defined here must not be used with a different meaning in requirements,
contracts, code, events, audit records or operational runbooks.

Every definition is governed by the Lot 0 baseline and the traceability rules
in `PAYMENT_IA1_BASELINE.md`.

---

## 2. Normative terminology

### `Payment`

The SIXPAY Aggregate Root representing the complete treatment of one unique
payment intention received from TRESOR PAY.

A Payment owns:

- its SIXPAY identity;
- its external references;
- the requested amount and currency;
- its current business status and business version;
- its normalized decisions and results;
- the minimal evidence that justified each decision;
- its failures and rejection reason;
- its legal state transitions.

A Payment does not represent a TRESOR PAY subscription, a bank account master,
an HTTP request, an Amplitude operation or a notification delivery process.

**Sources:** `PAY-BASE-001`, `PAY-BASE-002`, `PAY-AI-005`.

### `Received payment` / `Payment received`

A Payment whose authenticated and semantically admissible TRESOR PAY order has
been durably recorded by SIXPAY with its identity, external reference,
idempotency evidence, amount, protected account references and initial status.

`RECEIVED` means only that SIXPAY has accepted responsibility for processing
the intention. It does **not** mean that authorization, banking verification,
funds control, debit, CUT credit or TFJ finality has succeeded.

The TRESOR PAY operation named `InitiateDebit` is normalized as a payment-order
submission. Its `endToEndId` is the external business reference; the operation
name must not be interpreted as proof that a debit already exists.

**Sources:** `PAY-CONTRACT-001`, `PAY-BASE-005`, `PAY-SRC-001` to
`PAY-SRC-012`.

### `TRESOR PAY authorization`

A canonical, minimized decision proving that TRESOR PAY has attested that the
external subscription is active and applicable to the declared customer,
bank, debtor account and payment reference.

For the MVP, the evidence originates from a short-lived asymmetric signed JWT
validated locally by Security and Integration. Payment consumes only the
normalized outcome and minimal proof metadata required for audit.

Payment never owns or stores the JWT, Subscription Key, subscription lifecycle,
JWKS document or authentication credentials.

**Sources:** `PAY-BASE-006` to `PAY-BASE-009`, `PAY-AI-012`.

### `Banking verification`

A fresh, canonical decision based on authoritative Amplitude facts confirming
whether the declared banking customer and debtor account are coherent and
eligible for further payment processing.

It covers customer existence and matching, NIU and required identity
consistency, debtor-account existence and ownership, account status, debit
blocks, oppositions and required KYC facts.

It excludes funds availability, posting, reversal and TFJ finality.

Only `VERIFIED` permits progression. `REJECTED` is a negative business fact.
`INDETERMINATE` is not approval.

**Sources:** `PAY-CONTRACT-006`, `PAY-BASE-010`, `PAY-BASE-011`,
`PAY-AI-013`.

### `Funds control`

A fresh, read-only banking decision determining whether the Payment amount can
be executed at the time of the check.

It may include available funds, debit restrictions, transaction limits, amount
and currency coherence and freshness. It creates no financial entry and is not
a reservation unless a separate approved reservation capability is invoked.

**Sources:** `PAY-CONTRACT-002`, `PAY-BASE-010`, `PAY-BASE-012`.

### `Bank posting`

The authoritative Amplitude financial operation that records the financial
effect of the Payment.

The preferred MVP semantic is one atomic operation that debits the debtor
account, credits the configured CUT account, returns one stable posting
identity and may also return leg-level references and outcomes.

**Sources:** `PAY-CONTRACT-002`, `PAY-BASE-013`, `PAY-BASE-017`.

### `Debit`

The confirmed debit leg of the bank posting: an authoritative banking fact that
the Payment amount has been charged to the debtor account.

`DEBITED` must not be inferred from request acceptance, funds control, timeout
or notification.

**Sources:** `PAY-CONTRACT-002`, `PAY-BASE-013`, `OPEN-BASE-010`.

### `CUT credit`

The confirmed credit leg of the bank posting: an authoritative banking fact
that the configured Compte Unique du Trésor has been credited.

The CUT account is resolved from protected bank configuration. A TRESOR PAY
value cannot create, replace or override it. `CUT_CREDITED` is not TFJ finality.

**Sources:** `PAY-CONTRACT-001`, `PAY-CONTRACT-002`, `PAY-BASE-014`,
`PAY-BASE-018`.

### `TFJ finality`

The definitive Treasury-integration state established only after SIXPAY has
durably persisted a successful Amplitude end-of-day result uniquely matched to
the Payment.

TFJ finality is not established by debit alone, CUT credit alone, immediate
notification, elapsed time or an unmatched result.

**Sources:** `PAY-CONTRACT-004`, `PAY-BASE-018` to `PAY-BASE-021`.

### `Notification`

A durable business intent emitted by Payment so that Notification can deliver
an immediate or final lifecycle result to TRESOR PAY.

Payment owns the intent and event identity. Notification owns transport,
attempts, retry, backoff, DLQ, acknowledgements and delivery replay.

A delivery failure never changes Payment financial state. The recommended model
is a delivery milestone or projection outside the main financial state machine.

**Sources:** `PAY-CONTRACT-003`, `PAY-CONTRACT-005`, `PAY-BASE-022`,
`PAY-BASE-023`, `OPEN-BASE-009`.

### `Replay`

The re-presentation of the same logical input or event identity without creating
a new business or financial effect.

Inbound Payment replay, integration-event replay and notification-delivery
replay are distinct. A replay never authorizes a second posting.

**Sources:** `PAY-CONTRACT-001`, `PAY-CONTRACT-003`, `PAY-BASE-016`,
`PAY-BASE-023`.

### `Recovery`

A controlled operational process that resumes or resolves an interrupted
workflow without inventing a new Payment intention or blindly repeating a
financial command.

It may retry a safe read, perform an authoritative lookup, republish an Outbox
event, retry notification delivery or continue after a known canonical result.

**Sources:** `PAY-BASE-015`, `PAY-BASE-016`, `PAY-AI-010`.

### `Uncertain banking outcome`

A state of knowledge in which SIXPAY cannot prove whether a financial command
was applied by Amplitude.

It means neither success nor failure and imposes financial replay prohibition,
authoritative lookup, a non-final resolution state and operational visibility.

**Sources:** `PAY-CONTRACT-002`, `PAY-BASE-015`, `PAY-BASE-016`.

### `Reversal`

A new, explicit and authorized compensating banking operation intended to
neutralize a previously confirmed or partially confirmed financial effect.

A reversal is not deletion of the posting. It has its own identity and audit
trail, requires approved authorization and is looked up if uncertain. TFJ delay
alone never triggers it.

`Reversal` is the canonical domain term; `extourne` is an accepted source
synonym.

**Sources:** `PAY-CONTRACT-002`, `PAY-BASE-021`, `OPEN-BASE-008`.

### `Business failure`

A known, deterministic negative outcome caused by a business rule or an
authoritative negative external fact.

Examples include inactive authorization, customer/account mismatch, blocked
account, insufficient funds, exceeded limit, invalid transition and conflicting
external reference.

**Sources:** `PAY-CONTRACT-001`, `PAY-CONTRACT-006`, `PAY-BASE-010`.

### `Technical failure`

A transport, infrastructure, cryptography, persistence or availability failure
that prevents an operation from completing or being interpreted.

Before a financial command it may safely stop the workflow. After a financial
command it may instead produce an uncertain banking outcome.

**Sources:** `PAY-BASE-015`, `PAY-AI-009`, `PAY-AI-010`, `OPEN-BASE-011`.

### `Definitive rejection`

A terminal Payment decision proving that processing cannot continue and that no
financial effect occurred.

It requires a stable rejection code, safe explanation, proof of no posting,
immutable audit and the appropriate notification intent.

**Sources:** `PAY-BASE-010`, `PAY-BASE-015`, `PAY-AI-006`.

---

## 3. Terms prohibited or requiring qualification

| Ambiguous term | Required replacement |
| --- | --- |
| `transaction` | `Payment`, `bank posting`, `posting leg` or `notification delivery` |
| `success` | Name the exact successful milestone |
| `failure` | `business failure`, `technical failure` or `uncertain banking outcome` |
| `retry` | `safe read retry`, `authoritative lookup`, `event replay`, `notification retry` or `financial resubmission` |
| `reference` | Name the exact reference type |
| `validated` | Name authorization, banking, funds or contract validation |
| `completed` | Name posting completion, delivery completion or TFJ finality |
| `InitiateDebit succeeded` | `Payment request accepted` unless bank debit is confirmed |

## 4. Canonical French-English mapping

| French source term | Canonical domain term |
| --- | --- |
| ordre de virement / ordre de débit | `Payment order` |
| paiement | `Payment` |
| prélèvement / débit | `Debit` when confirmed |
| compte donneur d'ordre | `Debtor account` |
| compte bénéficiaire / CUT | `Configured CUT account` |
| comptabilisation / écriture | `Bank posting` |
| référence TRESOR PAY / `endToEndId` | `ExternalPaymentReference` |
| extourne | `Reversal` |
| rapprochement TFJ | `End-of-day reconciliation` |
| confirmation définitive | `TFJ finality` |
| reprise | `Recovery` |
| rejeu | `Replay` |
| résultat inconnu | `Uncertain banking outcome` |

## 5. Exit checklist

- [ ] Every major term has exactly one definition.
- [ ] `InitiateDebit` is not interpreted as proof of debit.
- [ ] Request acceptance, posting and TFJ finality are distinct.
- [ ] Replay, recovery and financial resubmission are distinct.
- [ ] Business failure, technical failure and uncertain outcome are distinct.
- [ ] Notification delivery is outside Payment financial state.
- [ ] Reversal is a separate compensating operation.
