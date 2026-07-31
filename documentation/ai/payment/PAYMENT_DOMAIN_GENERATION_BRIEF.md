# SIXPAY CONNECT — Payment Domain Generation Brief

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.5 — Commands and Business Operations`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `LOT_2_5_DRAFT_PENDING_VALIDATION`  
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
- `PAYMENT_INVARIANT_CATALOGUE.md`
- `PAYMENT_INVARIANT_CATALOGUE.yaml`
- `PAYMENT_COMMAND_CATALOGUE.md`
- `PAYMENT_COMMAND_CATALOGUE.yaml`
- `PAYMENT_STATE_MACHINE.yaml`
- `PAYMENT_DOMAIN_MODEL.md`
- `AI_CONTEXT_MANIFEST.yaml`

## 2. Prepared model

Completed:

- Aggregate Root;
- identifiers and Value Objects;
- business evidence snapshots;
- 76 invariants;
- 16 application commands;
- 17 aggregate operations including reconstitution;
- 17 Payment statuses;
- 38 legal state transitions.

Pending:

- final event catalogue;
- policies and Domain Services;
- final acceptance/model validation.

## 3. Command boundary

Commands are immutable application records.

Handlers own:

- command metadata;
- deduplication;
- expected-version checks;
- actor authorization;
- repository transactions;
- command receipts.

Payment receives only typed domain arguments.

Invalid operations raise a stable domain exception and produce no mutation,
version increment, event, audit transition or Outbox intent.

## 4. Aggregate operations

```text
receive
reconstitute
startAuthorizationChecking
recordAuthorizationDecision
recordBankingVerification
recordFundsControl
recordTreasuryAccountResolution
authorizePosting
recordPostingOutcome
resolvePostingOutcome
recordMatchedEndOfDayConfirmation
authorizeReversal
recordReversalOutcome
resolveReversalOutcome
reject
recordRecoverableFailure
failWithoutFinancialEffect
```

## 5. State-machine corrections

The IA-1 machine:

- separates banking verification from funds control;
- removes `NOTIFIED`;
- uses `POSTED_PENDING_TFJ` after complete posting;
- splits posting and reversal unknown outcomes;
- preserves `DEBIT_CONFIRMED` only for proven partial posting;
- requires evidence-driven `REJECTED` versus `FAILED`;
- requires matched TFJ `INTEGRATED` for finality.

## 6. Financial operation rules

- one logical posting instruction per Payment;
- posting instruction durable before network execution;
- exact retry reuses instruction identity and idempotency key;
- unknown result permits lookup only;
- partial effect requires reversal handling;
- reversal requires separate authorization and instruction identity;
- original posting evidence is never replaced.

## 7. Notification and process rules

Notification and external calls are not Payment commands.

Payment registers facts for:

- banking verification;
- funds control;
- Treasury account resolution;
- posting;
- posting lookup;
- immediate result availability;
- TFJ tracking;
- reversal;
- reversal lookup.

Final event names and safe schemas are produced in Lot 2.6.

## 8. Closed decisions

`PAY-DEC-IA1-033` through `PAY-DEC-IA1-040` are defined in
`PAYMENT_COMMAND_CATALOGUE.md`.

## 9. Traceability ranges

```text
PAY-CMD-001 ... PAY-CMD-016
PAY-OP-001  ... PAY-OP-017
PAY-TR-001  ... PAY-TR-038
PAY-INV-001 ... PAY-INV-076
```

## 10. Authorized Lot 2.5 modifications

```text
documentation/ai/payment/PAYMENT_COMMAND_CATALOGUE.md
documentation/ai/payment/PAYMENT_COMMAND_CATALOGUE.yaml
documentation/ai/payment/PAYMENT_STATE_MACHINE.yaml
documentation/ai/payment/PAYMENT_VALUE_OBJECT_CATALOGUE.md
documentation/ai/payment/PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md
documentation/ai/payment/PAYMENT_AGGREGATE_ROOT.md
documentation/ai/payment/PAYMENT_DOMAIN_MODEL.md
documentation/ai/payment/PAYMENT_DOMAIN_GENERATION_BRIEF.md
documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml
```

Implementation, contracts, architecture, requirements and event catalogue are
not modified.

## 11. Verdict

```text
IA-1 LOT 2.5 COMMANDS AND BUSINESS OPERATIONS PREPARED
COMMAND COUNT: 16
AGGREGATE OPERATION COUNT: 17
PAYMENT STATUS COUNT: 17
STATE TRANSITION COUNT: 38
STATUS: DRAFT_PENDING_VALIDATION
NEXT: LOT 2.6 — DOMAIN EVENTS
CODE GENERATION: FORBIDDEN
```
