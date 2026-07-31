# SIXPAY CONNECT — Payment Commands and Aggregate Operations

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.6 — Domain Events`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `DOMAIN_EVENTS_BOUND`  
> **Code generation:** **FORBIDDEN**

## 1. Purpose

This catalogue retains the 16 Lot 2.5 application commands and 17 named
aggregate operations, now bound to the final Lot 2.6 domain event IDs.

Commands remain application records and never enter the aggregate.

## 2. Common command protocol

Every command uses:

```text
commandId
operationCorrelationId
requestedAt
paymentId?
expectedBusinessVersion?
causationReference?
sourceEventId?
actorReference?
```

Handlers own command deduplication, authorization, version checking,
transactions and command receipts.

The aggregate receives only typed domain arguments.

## 3. Commands and event bindings

| Command | Operation | Version mode | Possible events |
| --- | --- | --- | --- |
| `PAY-CMD-001 ReceivePaymentCommand` | `Payment.receive` | `NONE` | `PAY-EVT-001` |
| `PAY-CMD-002 StartAuthorizationCheckingCommand` | `Payment.startAuthorizationChecking` | `REQUIRED` | `PAY-EVT-002` |
| `PAY-CMD-003 ApplyAuthorizationDecisionCommand` | `Payment.recordAuthorizationDecision` | `STATE_AND_EVIDENCE_GUARDED` | `PAY-EVT-003`, `PAY-EVT-004`, `PAY-EVT-005`, `PAY-EVT-006` |
| `PAY-CMD-004 ApplyBankingVerificationCommand` | `Payment.recordBankingVerification` | `STATE_AND_EVIDENCE_GUARDED` | `PAY-EVT-007`, `PAY-EVT-008`, `PAY-EVT-005`, `PAY-EVT-006`, `PAY-EVT-009` |
| `PAY-CMD-005 ApplyFundsControlCommand` | `Payment.recordFundsControl` | `STATE_AND_EVIDENCE_GUARDED` | `PAY-EVT-010`, `PAY-EVT-011`, `PAY-EVT-005`, `PAY-EVT-006`, `PAY-EVT-009` |
| `PAY-CMD-006 ApplyTreasuryAccountResolutionCommand` | `Payment.recordTreasuryAccountResolution` | `STATE_AND_EVIDENCE_GUARDED` | `PAY-EVT-012`, `PAY-EVT-013`, `PAY-EVT-005`, `PAY-EVT-006` |
| `PAY-CMD-007 AuthorizePostingCommand` | `Payment.authorizePosting` | `REQUIRED` | `PAY-EVT-014`, `PAY-EVT-015` |
| `PAY-CMD-008 ApplyPostingOutcomeCommand` | `Payment.recordPostingOutcome` | `STATE_AND_EVIDENCE_GUARDED` | `PAY-EVT-016`, `PAY-EVT-006`, `PAY-EVT-017`, `PAY-EVT-018`, `PAY-EVT-019`, `PAY-EVT-005`, `PAY-EVT-032`, `PAY-EVT-020` |
| `PAY-CMD-009 ResolvePostingOutcomeCommand` | `Payment.resolvePostingOutcome` | `STATE_AND_EVIDENCE_GUARDED` | `PAY-EVT-021`, `PAY-EVT-006`, `PAY-EVT-017`, `PAY-EVT-018`, `PAY-EVT-005`, `PAY-EVT-032`, `PAY-EVT-020` |
| `PAY-CMD-010 ApplyEndOfDayConfirmationCommand` | `Payment.recordMatchedEndOfDayConfirmation` | `STATE_AND_EVIDENCE_GUARDED` | `PAY-EVT-022`, `PAY-EVT-023`, `PAY-EVT-024`, `PAY-EVT-020`, `PAY-EVT-006`, `PAY-EVT-025` |
| `PAY-CMD-011 AuthorizeReversalCommand` | `Payment.authorizeReversal` | `REQUIRED` | `PAY-EVT-026`, `PAY-EVT-027` |
| `PAY-CMD-012 ApplyReversalOutcomeCommand` | `Payment.recordReversalOutcome` | `STATE_AND_EVIDENCE_GUARDED` | `PAY-EVT-028`, `PAY-EVT-033`, `PAY-EVT-029`, `PAY-EVT-030`, `PAY-EVT-020` |
| `PAY-CMD-013 ResolveReversalOutcomeCommand` | `Payment.resolveReversalOutcome` | `STATE_AND_EVIDENCE_GUARDED` | `PAY-EVT-031`, `PAY-EVT-033`, `PAY-EVT-029`, `PAY-EVT-020` |
| `PAY-CMD-014 RejectPaymentCommand` | `Payment.reject` | `REQUIRED` | `PAY-EVT-005`, `PAY-EVT-006` |
| `PAY-CMD-015 RecordRecoverableFailureCommand` | `Payment.recordRecoverableFailure` | `STATE_AND_EVIDENCE_GUARDED` | `PAY-EVT-009`, `PAY-EVT-006` |
| `PAY-CMD-016 FailWithoutFinancialEffectCommand` | `Payment.failWithoutFinancialEffect` | `REQUIRED` | `PAY-EVT-032`, `PAY-EVT-006` |
## 4. Detailed command facts

The `possibleFactKinds` list on each command is the union of all facts from its
legal state-machine branches. Actual emitted events depend on the selected
outcome and transition.

Notable Lot 2.6 corrections:

- banking/funds indeterminate and recoverable pre-financial failures also
  register `PaymentImmediateResultAvailable` with `PROCESSING`;
- debit-only outcomes register an immediate `PROCESSING` result;
- conclusive reversal rejection/not-allowed results register
  `PaymentReversalResultAvailable`;
- successful reversal registers both `PaymentReversed` and the reversal result
  intent.

## 5. Aggregate operations

| ID | Operation | Conceptual signature |
| --- | --- | --- |
| `PAY-OP-001` | `receive` | `static Payment receive(PaymentId id, PublicPaymentReference publicReference, NewPaymentIntent intent, Instant now)` |
| `PAY-OP-002` | `startAuthorizationChecking` | `void startAuthorizationChecking(Instant now)` |
| `PAY-OP-003` | `recordAuthorizationDecision` | `void recordAuthorizationDecision(AuthorizationEvidenceSnapshot evidence, PaymentFailure rejectionFailure, Instant now)` |
| `PAY-OP-004` | `recordBankingVerification` | `void recordBankingVerification(BankingVerificationSnapshot evidence, PaymentFailure failure, Instant now)` |
| `PAY-OP-005` | `recordFundsControl` | `void recordFundsControl(FundsControlSnapshot evidence, PaymentFailure failure, Instant now)` |
| `PAY-OP-006` | `recordTreasuryAccountResolution` | `void recordTreasuryAccountResolution(TreasuryAccountResolutionSnapshot evidence, TreasuryAccountReference resolvedAccount, PaymentFailure failure, Instant now)` |
| `PAY-OP-007` | `authorizePosting` | `void authorizePosting(PostingInstructionIdentity instruction, Instant now)` |
| `PAY-OP-008` | `recordPostingOutcome` | `void recordPostingOutcome(PostingOutcomeSnapshot evidence, PaymentFailure failure, Instant now)` |
| `PAY-OP-009` | `resolvePostingOutcome` | `void resolvePostingOutcome(PostingOutcomeSnapshot evidence, PaymentFailure failure, Instant now)` |
| `PAY-OP-010` | `recordMatchedEndOfDayConfirmation` | `void recordMatchedEndOfDayConfirmation(EndOfDayConfirmationSnapshot evidence, PaymentFailure failure, Instant now)` |
| `PAY-OP-011` | `authorizeReversal` | `void authorizeReversal(ReversalInstructionIdentity instruction, ReversalAuthorizationEvidence authorization, Instant now)` |
| `PAY-OP-012` | `recordReversalOutcome` | `void recordReversalOutcome(ReversalSnapshot evidence, PaymentFailure failure, Instant now)` |
| `PAY-OP-013` | `resolveReversalOutcome` | `void resolveReversalOutcome(ReversalSnapshot evidence, PaymentFailure failure, Instant now)` |
| `PAY-OP-014` | `reject` | `void reject(PaymentFailure rejection, Instant finalizedAt)` |
| `PAY-OP-015` | `recordRecoverableFailure` | `void recordRecoverableFailure(PaymentFailure failure, Instant now)` |
| `PAY-OP-016` | `failWithoutFinancialEffect` | `void failWithoutFinancialEffect(PaymentFailure failure, Instant finalizedAt)` |
| `PAY-OP-017` | `reconstitute` | `static Payment reconstitute(PaymentState state)` |
## 6. Event creation rule

A successful real mutation registers the ordered events listed by its
state-machine transition.

A no-op replay, rejected operation, conflict, stale version or reconstitution
registers no event.

Commands do not publish directly. The application transaction persists:

```text
Payment state
immutable audit
Outbox events
command receipt
```

## 7. External process boundary

Events request work from Security, Customer, Accounting, Integration and
Notification after commit.

The Payment aggregate performs no network, broker, database or notification
delivery operation.

## 8. Catalogue ranges

```text
PAY-CMD-001 ... PAY-CMD-016
PAY-OP-001  ... PAY-OP-017
PAY-EVT-001 ... PAY-EVT-033
```

## 9. Verdict

```text
COMMANDS: PREPARED
OPERATIONS: PREPARED
DOMAIN EVENT BINDINGS: PREPARED
NEXT: LOT 2.7 — POLICIES AND DOMAIN SERVICES
CODE GENERATION: FORBIDDEN
```
