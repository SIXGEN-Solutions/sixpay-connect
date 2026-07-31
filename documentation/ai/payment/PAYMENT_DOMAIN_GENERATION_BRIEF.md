# SIXPAY CONNECT — Payment Domain Generation Brief

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.3 — Snapshots and Business Evidence`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `LOT_2_3_DRAFT_PENDING_VALIDATION`  
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
- `PAYMENT_DOMAIN_MODEL.md`
- `PAYMENT_BANK_POSTING_REFERENCE_DECISION.md`
- `AI_CONTEXT_MANIFEST.yaml`
- `documentation/contracts/CONTRACT_REGISTRY.yaml`

## 2. Aggregate and Value Object baseline

`Payment` is the sole write Aggregate Root for one logical TRESOR PAY payment
intention.

Lot 2.2 identifiers and Value Objects remain normative. Shared-kernel `Money`
is reused unchanged.

## 3. Snapshot rule

Payment stores only the current immutable, minimized evidence that directly
supports its current decisions.

The complete evidence history belongs to append-only audit/reporting.

Every snapshot contains common metadata:

```text
sourceSystem
correlationId
observationChannel
evidenceFingerprint
observedAt
acceptedAt
```

Raw external payloads never enter Payment.

## 4. Snapshot catalogue

| Snapshot | Purpose |
| --- | --- |
| `AuthorizationEvidenceSnapshot` | Prove signed TRESOR PAY authorization bindings |
| `BankingVerificationSnapshot` | Prove customer/account/KYC checks used by Payment |
| `FundsControlSnapshot` | Prove exact amount/account execution checks |
| `TreasuryAccountResolutionSnapshot` | Prove protected CUT configuration resolution |
| `PostingOutcomeSnapshot` | Prove known, partial, rejected or unknown posting outcome |
| `EndOfDayConfirmationSnapshot` | Prove uniquely matched final TFJ result |
| `ReversalSnapshot` | Prove reversal authorization and current outcome |

## 5. Minimization rules

- no JWT, signature, raw claim or Subscription Key;
- no customer identity/KYC value;
- no full bank account;
- no available balance;
- no raw provider payload;
- no retry-attempt history;
- no unmatched TFJ result;
- no notification delivery state;
- no provider free-form technical error.

## 6. Posting and TFJ decisions

Posting evidence preserves:

- financial-command idempotency identity;
- outcome;
- principal/leg references;
- debit and CUT-credit leg statuses;
- exact amount;
- business date;
- next action;
- observation source/channel.

`UNKNOWN` never means failure and requires authoritative resolution.

Only an authenticated, durably persisted and uniquely matched final TFJ result
enters Payment. TFJ `PENDING`, unmatched, ambiguous or conflicting evidence
remains in Accounting/quarantine.

## 7. Replay and replacement

- same evidence identity + same fingerprint: no-op;
- same identity + different fingerprint: conflict, audit and quarantine when
  financial/TFJ;
- replacement requires a permitted lifecycle, equal immutable bindings and a
  more authoritative or conclusive result;
- terminal evidence is not silently replaced.

## 8. Closed Lot 2.3 decisions

`PAY-DEC-IA1-017` through `PAY-DEC-IA1-026` are defined in
`PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md`.

Structural snapshot invariants are `PAY-SNAP-001` through `PAY-SNAP-018`.

## 9. Deferred scope

| Lot | Deferred subject |
| --- | --- |
| 2.4 | Complete cross-object and lifecycle invariants |
| 2.5 | Commands and aggregate operations |
| 2.6 | Domain Events and safe payloads |
| 2.7 | Freshness, matching, resolver policies and Domain Services |
| 2.8 | Final model validation |

## 10. Traceability prefixes

```text
PAY-BASE-*
PAY-BOUND-*
PAY-POSTREF-*
PAY-AGG-*
PAY-VO-*
PAY-SNAP-*
PAY-DEC-*
PAY-CONTRACT-*
PAY-AI-*
PAY-SRC-*
SRC-*
OPEN-*
```

## 11. Authorized Lot 2.3 modifications

```text
documentation/ai/payment/PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md
documentation/ai/payment/PAYMENT_VALUE_OBJECT_CATALOGUE.md
documentation/ai/payment/PAYMENT_AGGREGATE_ROOT.md
documentation/ai/payment/PAYMENT_DOMAIN_MODEL.md
documentation/ai/payment/PAYMENT_DOMAIN_GENERATION_BRIEF.md
documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml
```

Implementation, contracts, architecture, requirements, state machine and event
catalogue are not modified in this lot.

## 12. Verdict

```text
IA-1 LOT 2.3 SNAPSHOTS AND BUSINESS EVIDENCE PREPARED
STATUS: DRAFT_PENDING_VALIDATION
NEXT: LOT 2.4 — INVARIANTS
CODE GENERATION: FORBIDDEN
```
