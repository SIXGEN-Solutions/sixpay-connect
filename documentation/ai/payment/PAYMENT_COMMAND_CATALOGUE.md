# SIXPAY CONNECT — Payment Commands and Aggregate Operations

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Lot:** `2.5 — Commands and Business Operations`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `DRAFT_PENDING_VALIDATION`  
> **Code generation:** **FORBIDDEN**

## 1. Purpose

This document defines:

- immutable application commands accepted by the Payment write model;
- named operations exposed by the `Payment` Aggregate Root;
- command metadata, idempotency and concurrency rules;
- legal state changes and outcome branches;
- the boundary between Payment operations and external processes.

It finalizes command and operation semantics without defining final domain-event
payloads. Event names, schemas and consumer contracts remain assigned to
Lot 2.6.

## 2. Command versus aggregate operation

A Payment command is an immutable application-layer request.

An aggregate operation is a named domain method that validates invariants,
changes aggregate state and registers domain facts.

```text
application command
        ↓ handler validates metadata / dedup / version
typed Value Objects and snapshots
        ↓
named Payment operation
        ↓
state mutation + domain facts
        ↓
state + audit + Outbox committed atomically
```

The aggregate never receives an application command object directly. The
handler unwraps the command and calls the typed domain operation.

Named mutation methods follow the Golden Partner convention:

- successful methods return normally and register domain facts;
- invalid state or invariant violations raise a stable
  `PaymentDomainException`;
- invalid operations leave the aggregate unchanged;
- there is no generic `setStatus` or `transitionTo(PaymentStatus)` public API.

**Decision:** `PAY-DEC-IA1-033`.

## 3. Common command metadata

Every command has `PaymentCommandMetadata`:

```text
PaymentCommandMetadata
├── PaymentCommandId commandId
├── CorrelationId operationCorrelationId
├── Instant requestedAt
├── PaymentId? paymentId
├── ExpectedBusinessVersion? expectedBusinessVersion
├── CommandCausationReference? causationReference
├── UUID? sourceEventId
└── CommandActorReference? actorReference
```

Rules:

1. `commandId` is non-nil and generated once.
2. Same `commandId` plus same command fingerprint returns the recorded command
   result.
3. Same `commandId` plus another fingerprint is a command conflict.
4. `expectedBusinessVersion` is mandatory for commands marked `REQUIRED`.
5. Asynchronous canonical-result commands may omit expected version but still
   require eligible aggregate state, evidence binding and optimistic locking.
6. The command registry is application/infrastructure state, not an unbounded
   collection inside Payment.
7. `operationCorrelationId` supports the current invocation; Payment domain
   events retain the original Payment lifecycle correlation according to the
   event policy.
8. Actor roles and credentials never enter Payment. Reversal authorization is
   represented by its approved evidence object.

**Decision:** `PAY-DEC-IA1-034`.

## 4. Handler transaction protocol

For a command on an existing Payment:

1. validate command metadata;
2. resolve command replay/conflict;
3. load Payment;
4. enforce expected version when required;
5. map only canonical domain types;
6. call one named aggregate operation;
7. persist aggregate state;
8. append immutable audit;
9. append Outbox intents for registered domain facts;
10. persist command receipt;
11. commit atomically.

No HTTP, Kafka or Amplitude call occurs inside this transaction.

Creation additionally commits the inbound idempotency record and uniqueness
claim with the new Payment.

External processes start only after commit from Outbox/domain facts.

## 5. Command result semantics

Application command receipts use these semantic outcomes:

```text
APPLIED
NO_OP_IDENTICAL_REPLAY
EXISTING_PAYMENT_REPLAY
REJECTED_INVALID_STATE
REJECTED_INVARIANT
STALE_VERSION
CONFLICT
```

The receipt may expose only:

- PaymentId;
- PublicPaymentReference;
- current PaymentStatus;
- resulting businessVersion;
- stable result code;
- safe message;
- commandId.

It never exposes a protected snapshot or raw external payload.

## 6. Command catalogue summary

| ID | Command | Aggregate operation | Version mode | Allowed states |
| --- | --- | --- | --- | --- |
| `PAY-CMD-001` | `ReceivePaymentCommand` | `Payment.receive` | `NONE` | Creation |
| `PAY-CMD-002` | `StartAuthorizationCheckingCommand` | `Payment.startAuthorizationChecking` | `REQUIRED` | RECEIVED |
| `PAY-CMD-003` | `ApplyAuthorizationDecisionCommand` | `Payment.recordAuthorizationDecision` | `STATE_AND_EVIDENCE_GUARDED` | AUTHORIZATION_CHECKING |
| `PAY-CMD-004` | `ApplyBankingVerificationCommand` | `Payment.recordBankingVerification` | `STATE_AND_EVIDENCE_GUARDED` | BANKING_VERIFICATION_PENDING |
| `PAY-CMD-005` | `ApplyFundsControlCommand` | `Payment.recordFundsControl` | `STATE_AND_EVIDENCE_GUARDED` | FUNDS_CONTROL_PENDING |
| `PAY-CMD-006` | `ApplyTreasuryAccountResolutionCommand` | `Payment.recordTreasuryAccountResolution` | `STATE_AND_EVIDENCE_GUARDED` | TREASURY_ACCOUNT_RESOLUTION_PENDING |
| `PAY-CMD-007` | `AuthorizePostingCommand` | `Payment.authorizePosting` | `REQUIRED` | APPROVED_FOR_POSTING |
| `PAY-CMD-008` | `ApplyPostingOutcomeCommand` | `Payment.recordPostingOutcome` | `STATE_AND_EVIDENCE_GUARDED` | POSTING_PENDING, DEBIT_CONFIRMED |
| `PAY-CMD-009` | `ResolvePostingOutcomeCommand` | `Payment.resolvePostingOutcome` | `STATE_AND_EVIDENCE_GUARDED` | POSTING_OUTCOME_UNKNOWN |
| `PAY-CMD-010` | `ApplyEndOfDayConfirmationCommand` | `Payment.recordMatchedEndOfDayConfirmation` | `STATE_AND_EVIDENCE_GUARDED` | POSTED_PENDING_TFJ |
| `PAY-CMD-011` | `AuthorizeReversalCommand` | `Payment.authorizeReversal` | `REQUIRED` | REVERSAL_REQUIRED |
| `PAY-CMD-012` | `ApplyReversalOutcomeCommand` | `Payment.recordReversalOutcome` | `STATE_AND_EVIDENCE_GUARDED` | REVERSAL_PENDING |
| `PAY-CMD-013` | `ResolveReversalOutcomeCommand` | `Payment.resolveReversalOutcome` | `STATE_AND_EVIDENCE_GUARDED` | REVERSAL_OUTCOME_UNKNOWN |
| `PAY-CMD-014` | `RejectPaymentCommand` | `Payment.reject` | `REQUIRED` | RECEIVED |
| `PAY-CMD-015` | `RecordRecoverableFailureCommand` | `Payment.recordRecoverableFailure` | `STATE_AND_EVIDENCE_GUARDED` | AUTHORIZATION_CHECKING, BANKING_VERIFICATION_PENDING, FUNDS_CONTROL_PENDING, TREASURY_ACCOUNT_RESOLUTION_PENDING |
| `PAY-CMD-016` | `FailWithoutFinancialEffectCommand` | `Payment.failWithoutFinancialEffect` | `REQUIRED` | RECEIVED, AUTHORIZATION_CHECKING, BANKING_VERIFICATION_PENDING, FUNDS_CONTROL_PENDING, TREASURY_ACCOUNT_RESOLUTION_PENDING, APPROVED_FOR_POSTING |

## 7. Detailed commands

### `PAY-CMD-001` — `ReceivePaymentCommand`

- **Kind:** `CREATION`
- **Aggregate operation:** `Payment.receive` (`PAY-OP-001`)
- **Version mode:** `NONE`
- **Allowed states:** No existing aggregate.

**Payload**

- `PaymentSource source`
- `ExternalPaymentReference externalPaymentReference`
- `ExternalSubscriptionReference externalSubscriptionReference`
- `PaymentRequestIdentity requestIdentity`
- `FinancialInstitutionCode financialInstitution`
- `DebtorAccountReference debtorAccount`
- `Money requestedAmount`
- `TreasuryAllocationIntent treasuryAllocationIntent`
- `Instant receivedAt`

**Generated by the handler before aggregate creation**

- `PaymentId`
- `PublicPaymentReference`

**Guard references**

`PAY-INV-001`, `PAY-INV-002`, `PAY-INV-003`, `PAY-INV-004`, `PAY-INV-005`, `PAY-INV-006`, `PAY-INV-007`, `PAY-INV-008`, `PAY-INV-019`, `PAY-INV-020`.

**Semantic outcomes**

- `APPLIED_RECEIVED`
- `EXISTING_PAYMENT_IDENTICAL_REPLAY`
- `EXTERNAL_REFERENCE_OR_IDEMPOTENCY_CONFLICT`

**Domain fact kinds registered when applicable**

- `PAYMENT_RECEIVED`

**Forbidden behavior**

- Authenticate credentials inside Payment.
- Call Amplitude before Payment is committed.
- Create another Payment for an identical replay.

### `PAY-CMD-002` — `StartAuthorizationCheckingCommand`

- **Kind:** `LIFECYCLE`
- **Aggregate operation:** `Payment.startAuthorizationChecking` (`PAY-OP-002`)
- **Version mode:** `REQUIRED`
- **Allowed states:** `RECEIVED`

**Payload**

- `Instant startedAt`

**Guard references**

`PAY-INV-010`, `PAY-INV-021`, `PAY-INV-069`, `PAY-INV-070`.

**Semantic outcomes**

- `APPLIED_AUTHORIZATION_CHECKING`
- `NO_OP_IDENTICAL_REPLAY`

**Domain fact kinds registered when applicable**

- `AUTHORIZATION_CHECKING_STARTED`

**Forbidden behavior**

- Perform JWT verification inside Payment.
- Start a banking call directly.

### `PAY-CMD-003` — `ApplyAuthorizationDecisionCommand`

- **Kind:** `CANONICAL_RESULT`
- **Aggregate operation:** `Payment.recordAuthorizationDecision` (`PAY-OP-003`)
- **Version mode:** `STATE_AND_EVIDENCE_GUARDED`
- **Allowed states:** `AUTHORIZATION_CHECKING`

**Payload**

- `AuthorizationEvidenceSnapshot evidence`
- `PaymentFailure? rejectionFailure`

**Guard references**

`PAY-INV-021`, `PAY-INV-022`, `PAY-INV-023`, `PAY-INV-024`, `PAY-INV-025`.

**Semantic outcomes**

- `APPROVED_TO_BANKING_VERIFICATION_PENDING`
- `REJECTED_TERMINAL`
- `NO_OP_IDENTICAL_EVIDENCE_REPLAY`
- `SECURITY_EVIDENCE_CONFLICT`

**Domain fact kinds registered when applicable**

- `AUTHORIZATION_DECISION_RECORDED`
- `BANKING_VERIFICATION_REQUESTED_IF_APPROVED`
- `PAYMENT_REJECTED_IF_REJECTED`
- `IMMEDIATE_RESULT_AVAILABLE_IF_REJECTED`

**Forbidden behavior**

- Accept a raw JWT or claims.
- Treat infrastructure failure as an authorization decision.

### `PAY-CMD-004` — `ApplyBankingVerificationCommand`

- **Kind:** `CANONICAL_RESULT`
- **Aggregate operation:** `Payment.recordBankingVerification` (`PAY-OP-004`)
- **Version mode:** `STATE_AND_EVIDENCE_GUARDED`
- **Allowed states:** `BANKING_VERIFICATION_PENDING`

**Payload**

- `BankingVerificationSnapshot evidence`
- `PaymentFailure? negativeOrIndeterminateFailure`

**Guard references**

`PAY-INV-027`, `PAY-INV-028`, `PAY-INV-029`, `PAY-INV-030`, `PAY-INV-031`.

**Semantic outcomes**

- `VERIFIED_TO_FUNDS_CONTROL_PENDING`
- `REJECTED_TERMINAL`
- `INDETERMINATE_REMAINS_PENDING`
- `NO_OP_IDENTICAL_EVIDENCE_REPLAY`
- `EVIDENCE_CONFLICT`

**Domain fact kinds registered when applicable**

- `BANKING_VERIFICATION_RECORDED`
- `FUNDS_CONTROL_REQUESTED_IF_VERIFIED`
- `PAYMENT_REJECTED_IF_REJECTED`
- `PROCESSING_DEFERRED_IF_INDETERMINATE`

**Forbidden behavior**

- Store KYC payloads.
- Use ObservedCustomer as approval evidence.
- Combine funds approval with this result.

### `PAY-CMD-005` — `ApplyFundsControlCommand`

- **Kind:** `CANONICAL_RESULT`
- **Aggregate operation:** `Payment.recordFundsControl` (`PAY-OP-005`)
- **Version mode:** `STATE_AND_EVIDENCE_GUARDED`
- **Allowed states:** `FUNDS_CONTROL_PENDING`

**Payload**

- `FundsControlSnapshot evidence`
- `PaymentFailure? negativeOrIndeterminateFailure`

**Guard references**

`PAY-INV-027`, `PAY-INV-032`, `PAY-INV-033`, `PAY-INV-034`.

**Semantic outcomes**

- `VERIFIED_TO_TREASURY_ACCOUNT_RESOLUTION_PENDING`
- `REJECTED_TERMINAL`
- `INDETERMINATE_REMAINS_PENDING`
- `NO_OP_IDENTICAL_EVIDENCE_REPLAY`
- `EVIDENCE_CONFLICT`

**Domain fact kinds registered when applicable**

- `FUNDS_CONTROL_RECORDED`
- `TREASURY_ACCOUNT_RESOLUTION_REQUESTED_IF_VERIFIED`
- `PAYMENT_REJECTED_IF_REJECTED`
- `PROCESSING_DEFERRED_IF_INDETERMINATE`

**Forbidden behavior**

- Store available balance.
- Authorize posting from stale or mismatched funds evidence.

### `PAY-CMD-006` — `ApplyTreasuryAccountResolutionCommand`

- **Kind:** `CANONICAL_RESULT`
- **Aggregate operation:** `Payment.recordTreasuryAccountResolution` (`PAY-OP-006`)
- **Version mode:** `STATE_AND_EVIDENCE_GUARDED`
- **Allowed states:** `TREASURY_ACCOUNT_RESOLUTION_PENDING`

**Payload**

- `TreasuryAccountResolutionSnapshot evidence`
- `TreasuryAccountReference? resolvedTreasuryAccount`
- `PaymentFailure? rejectionFailure`

**Guard references**

`PAY-INV-035`, `PAY-INV-036`, `PAY-INV-037`, `PAY-INV-038`.

**Semantic outcomes**

- `RESOLVED_TO_APPROVED_FOR_POSTING`
- `REJECTED_TERMINAL`
- `NO_OP_IDENTICAL_EVIDENCE_REPLAY`
- `CONFIGURATION_CONFLICT`

**Domain fact kinds registered when applicable**

- `TREASURY_ACCOUNT_RESOLUTION_RECORDED`
- `PAYMENT_APPROVED_FOR_POSTING_IF_RESOLVED`
- `PAYMENT_REJECTED_IF_REJECTED`

**Forbidden behavior**

- Construct a CUT account from inbound TRESOR PAY data.
- Expose a Treasury account token in an event.

### `PAY-CMD-007` — `AuthorizePostingCommand`

- **Kind:** `FINANCIAL_AUTHORIZATION`
- **Aggregate operation:** `Payment.authorizePosting` (`PAY-OP-007`)
- **Version mode:** `REQUIRED`
- **Allowed states:** `APPROVED_FOR_POSTING`

**Payload**

- `PostingInstructionIdentity instruction`
- `Instant authorizedAt`

**Guard references**

`PAY-INV-032`, `PAY-INV-035`, `PAY-INV-038`, `PAY-INV-039`, `PAY-INV-040`, `PAY-INV-041`.

**Semantic outcomes**

- `APPLIED_POSTING_PENDING`
- `NO_OP_SAME_INSTRUCTION_REPLAY`
- `SECOND_OR_CONFLICTING_POSTING_REJECTED`

**Domain fact kinds registered when applicable**

- `POSTING_AUTHORIZED`
- `POSTING_REQUESTED`

**Forbidden behavior**

- Call Amplitude inside the aggregate transaction.
- Generate another posting identity for the same Payment.

### `PAY-CMD-008` — `ApplyPostingOutcomeCommand`

- **Kind:** `CANONICAL_FINANCIAL_RESULT`
- **Aggregate operation:** `Payment.recordPostingOutcome` (`PAY-OP-008`)
- **Version mode:** `STATE_AND_EVIDENCE_GUARDED`
- **Allowed states:** `POSTING_PENDING`, `DEBIT_CONFIRMED`

**Payload**

- `PostingOutcomeSnapshot evidence`
- `PaymentFailure? failure`

**Guard references**

`PAY-INV-041`, `PAY-INV-042`, `PAY-INV-043`, `PAY-INV-044`, `PAY-INV-045`, `PAY-INV-046`, `PAY-INV-047`, `PAY-INV-048`, `PAY-INV-049`, `PAY-INV-050`.

**Semantic outcomes**

- `COMPLETED_TO_POSTED_PENDING_TFJ`
- `DEBIT_ONLY_TO_DEBIT_CONFIRMED`
- `UNKNOWN_TO_POSTING_OUTCOME_UNKNOWN`
- `BUSINESS_REJECTED_WITHOUT_EFFECT`
- `TECHNICAL_FAILED_WITHOUT_EFFECT`
- `PARTIAL_EFFECT_TO_REVERSAL_REQUIRED`
- `NO_OP_IDENTICAL_EVIDENCE_REPLAY`
- `FINANCIAL_EVIDENCE_CONFLICT`

**Domain fact kinds registered when applicable**

- `POSTING_OUTCOME_RECORDED`
- `IMMEDIATE_RESULT_AVAILABLE`
- `TFJ_TRACKING_REQUESTED_IF_COMPLETED`
- `POSTING_OUTCOME_LOOKUP_REQUESTED_IF_UNKNOWN`
- `REVERSAL_REVIEW_REQUESTED_IF_REQUIRED`

**Forbidden behavior**

- Infer TFJ finality.
- Blindly resubmit the posting.
- Classify an uncertain result as failure.

### `PAY-CMD-009` — `ResolvePostingOutcomeCommand`

- **Kind:** `AUTHORITATIVE_LOOKUP_RESULT`
- **Aggregate operation:** `Payment.resolvePostingOutcome` (`PAY-OP-009`)
- **Version mode:** `STATE_AND_EVIDENCE_GUARDED`
- **Allowed states:** `POSTING_OUTCOME_UNKNOWN`

**Payload**

- `PostingOutcomeSnapshot authoritativeEvidence`
- `PaymentFailure? failure`

**Guard references**

`PAY-INV-042`, `PAY-INV-043`, `PAY-INV-044`, `PAY-INV-045`, `PAY-INV-046`, `PAY-INV-047`, `PAY-INV-048`.

**Semantic outcomes**

- `RESOLVED_COMPLETED`
- `RESOLVED_DEBIT_ONLY`
- `RESOLVED_BUSINESS_REJECTED_WITHOUT_EFFECT`
- `RESOLVED_TECHNICAL_FAILED_WITHOUT_EFFECT`
- `RESOLVED_REVERSAL_REQUIRED`
- `NO_OP_STILL_UNKNOWN_OR_IDENTICAL_REPLAY`
- `FINANCIAL_EVIDENCE_CONFLICT`

**Domain fact kinds registered when applicable**

- `POSTING_OUTCOME_RESOLVED`
- `IMMEDIATE_RESULT_AVAILABLE_IF_CONCLUSIVE`
- `TFJ_TRACKING_REQUESTED_IF_COMPLETED`
- `REVERSAL_REVIEW_REQUESTED_IF_REQUIRED`

**Forbidden behavior**

- Create a replacement instruction.
- Resolve from non-authoritative operational assumptions.

### `PAY-CMD-010` — `ApplyEndOfDayConfirmationCommand`

- **Kind:** `CANONICAL_TFJ_RESULT`
- **Aggregate operation:** `Payment.recordMatchedEndOfDayConfirmation` (`PAY-OP-010`)
- **Version mode:** `STATE_AND_EVIDENCE_GUARDED`
- **Allowed states:** `POSTED_PENDING_TFJ`

**Payload**

- `EndOfDayConfirmationSnapshot evidence`
- `PaymentFailure? reconciliationFailure`

**Guard references**

`PAY-INV-053`, `PAY-INV-054`, `PAY-INV-055`, `PAY-INV-056`, `PAY-INV-057`, `PAY-INV-058`.

**Semantic outcomes**

- `INTEGRATED_TERMINAL`
- `FAILED_REQUIRES_REVERSAL`
- `FAILED_REQUIRES_MANUAL_RECONCILIATION_REMAINS_PENDING`
- `NO_OP_IDENTICAL_REPLAY`
- `TFJ_CONFLICT_QUARANTINED`

**Domain fact kinds registered when applicable**

- `TFJ_CONFIRMATION_RECORDED`
- `TREASURY_INTEGRATION_CONFIRMED_IF_INTEGRATED`
- `FINAL_RESULT_AVAILABLE_IF_INTEGRATED`
- `REVERSAL_REVIEW_REQUESTED_IF_REQUIRED`
- `TREASURY_RECONCILIATION_REQUIRED_IF_MANUAL`

**Forbidden behavior**

- Accept PENDING or unmatched TFJ data.
- Map TFJ FAILED automatically to Payment FAILED.

### `PAY-CMD-011` — `AuthorizeReversalCommand`

- **Kind:** `FINANCIAL_AUTHORIZATION`
- **Aggregate operation:** `Payment.authorizeReversal` (`PAY-OP-011`)
- **Version mode:** `REQUIRED`
- **Allowed states:** `REVERSAL_REQUIRED`

**Payload**

- `ReversalInstructionIdentity instruction`
- `ReversalAuthorizationEvidence authorization`
- `Instant authorizedAt`

**Guard references**

`PAY-INV-059`, `PAY-INV-060`, `PAY-INV-064`.

**Semantic outcomes**

- `APPLIED_REVERSAL_PENDING`
- `NO_OP_SAME_ACTIVE_INSTRUCTION_REPLAY`
- `UNAUTHORIZED_OR_CONFLICTING_REVERSAL_REJECTED`

**Domain fact kinds registered when applicable**

- `REVERSAL_AUTHORIZED`
- `REVERSAL_REQUESTED`

**Forbidden behavior**

- Reverse automatically.
- Reuse the original posting idempotency key.
- Delete original posting evidence.

### `PAY-CMD-012` — `ApplyReversalOutcomeCommand`

- **Kind:** `CANONICAL_FINANCIAL_RESULT`
- **Aggregate operation:** `Payment.recordReversalOutcome` (`PAY-OP-012`)
- **Version mode:** `STATE_AND_EVIDENCE_GUARDED`
- **Allowed states:** `REVERSAL_PENDING`

**Payload**

- `ReversalSnapshot evidence`
- `PaymentFailure? failure`

**Guard references**

`PAY-INV-060`, `PAY-INV-061`, `PAY-INV-062`, `PAY-INV-063`.

**Semantic outcomes**

- `REVERSED_TERMINAL`
- `UNKNOWN_TO_REVERSAL_OUTCOME_UNKNOWN`
- `REJECTED_OR_NOT_ALLOWED_TO_REVERSAL_REQUIRED`
- `NO_OP_IDENTICAL_EVIDENCE_REPLAY`
- `REVERSAL_EVIDENCE_CONFLICT`

**Domain fact kinds registered when applicable**

- `REVERSAL_OUTCOME_RECORDED`
- `REVERSAL_RESULT_AVAILABLE_IF_CONCLUSIVE`
- `REVERSAL_OUTCOME_LOOKUP_REQUESTED_IF_UNKNOWN`

**Forbidden behavior**

- Treat rejection as absence of the original effect.
- Blindly resubmit an unknown reversal.

### `PAY-CMD-013` — `ResolveReversalOutcomeCommand`

- **Kind:** `AUTHORITATIVE_LOOKUP_RESULT`
- **Aggregate operation:** `Payment.resolveReversalOutcome` (`PAY-OP-013`)
- **Version mode:** `STATE_AND_EVIDENCE_GUARDED`
- **Allowed states:** `REVERSAL_OUTCOME_UNKNOWN`

**Payload**

- `ReversalSnapshot authoritativeEvidence`
- `PaymentFailure? failure`

**Guard references**

`PAY-INV-060`, `PAY-INV-061`, `PAY-INV-062`, `PAY-INV-063`.

**Semantic outcomes**

- `RESOLVED_REVERSED_TERMINAL`
- `RESOLVED_REJECTED_OR_NOT_ALLOWED`
- `NO_OP_STILL_UNKNOWN_OR_IDENTICAL_REPLAY`
- `REVERSAL_EVIDENCE_CONFLICT`

**Domain fact kinds registered when applicable**

- `REVERSAL_OUTCOME_RESOLVED`
- `REVERSAL_RESULT_AVAILABLE_IF_CONCLUSIVE`

**Forbidden behavior**

- Create a new reversal instruction during lookup.
- Overwrite the original posting reference.

### `PAY-CMD-014` — `RejectPaymentCommand`

- **Kind:** `TERMINAL_DECISION`
- **Aggregate operation:** `Payment.reject` (`PAY-OP-014`)
- **Version mode:** `REQUIRED`
- **Allowed states:** `RECEIVED`

**Payload**

- `PaymentFailure businessOrSecurityRejection`
- `Instant finalizedAt`

**Guard references**

`PAY-INV-020`, `PAY-INV-065`, `PAY-INV-068`.

**Semantic outcomes**

- `REJECTED_TERMINAL`
- `NO_OP_IDENTICAL_TERMINAL_REPLAY`

**Domain fact kinds registered when applicable**

- `PAYMENT_REJECTED`
- `IMMEDIATE_RESULT_AVAILABLE`

**Forbidden behavior**

- Use after financial submission.
- Use a technical failure category.
- Reopen the Payment later.

### `PAY-CMD-015` — `RecordRecoverableFailureCommand`

- **Kind:** `NON_TERMINAL_FAILURE`
- **Aggregate operation:** `Payment.recordRecoverableFailure` (`PAY-OP-015`)
- **Version mode:** `STATE_AND_EVIDENCE_GUARDED`
- **Allowed states:** `AUTHORIZATION_CHECKING`, `BANKING_VERIFICATION_PENDING`, `FUNDS_CONTROL_PENDING`, `TREASURY_ACCOUNT_RESOLUTION_PENDING`

**Payload**

- `PaymentFailure recoverableFailure`

**Guard references**

`PAY-INV-024`, `PAY-INV-033`, `PAY-INV-068`, `PAY-INV-069`.

**Semantic outcomes**

- `FAILURE_RECORDED_STATUS_UNCHANGED`
- `NO_OP_IDENTICAL_FAILURE_REPLAY`

**Domain fact kinds registered when applicable**

- `PAYMENT_PROCESSING_DEFERRED`

**Forbidden behavior**

- Use for an uncertain financial write.
- Use NOT_RETRYABLE without terminal resolution.

### `PAY-CMD-016` — `FailWithoutFinancialEffectCommand`

- **Kind:** `TERMINAL_DECISION`
- **Aggregate operation:** `Payment.failWithoutFinancialEffect` (`PAY-OP-016`)
- **Version mode:** `REQUIRED`
- **Allowed states:** `RECEIVED`, `AUTHORIZATION_CHECKING`, `BANKING_VERIFICATION_PENDING`, `FUNDS_CONTROL_PENDING`, `TREASURY_ACCOUNT_RESOLUTION_PENDING`, `APPROVED_FOR_POSTING`

**Payload**

- `PaymentFailure technicalFailure`
- `Instant finalizedAt`

**Guard references**

`PAY-INV-043`, `PAY-INV-066`, `PAY-INV-068`.

**Semantic outcomes**

- `FAILED_TERMINAL`
- `NO_OP_IDENTICAL_TERMINAL_REPLAY`

**Domain fact kinds registered when applicable**

- `PAYMENT_FAILED_WITHOUT_FINANCIAL_EFFECT`
- `IMMEDIATE_RESULT_AVAILABLE`

**Forbidden behavior**

- Use when posting may have been submitted.
- Use for a business rejection.
- Use for partial or uncertain effect.

## 8. Aggregate operation catalogue

| ID | Operation | Conceptual signature | Command |
| --- | --- | --- | --- |
| `PAY-OP-001` | `receive` | `static Payment receive(PaymentId id, PublicPaymentReference publicReference, NewPaymentIntent intent, Instant now)` | `PAY-CMD-001` |
| `PAY-OP-002` | `startAuthorizationChecking` | `void startAuthorizationChecking(Instant now)` | `PAY-CMD-002` |
| `PAY-OP-003` | `recordAuthorizationDecision` | `void recordAuthorizationDecision(AuthorizationEvidenceSnapshot evidence, PaymentFailure rejectionFailure, Instant now)` | `PAY-CMD-003` |
| `PAY-OP-004` | `recordBankingVerification` | `void recordBankingVerification(BankingVerificationSnapshot evidence, PaymentFailure failure, Instant now)` | `PAY-CMD-004` |
| `PAY-OP-005` | `recordFundsControl` | `void recordFundsControl(FundsControlSnapshot evidence, PaymentFailure failure, Instant now)` | `PAY-CMD-005` |
| `PAY-OP-006` | `recordTreasuryAccountResolution` | `void recordTreasuryAccountResolution(TreasuryAccountResolutionSnapshot evidence, TreasuryAccountReference resolvedAccount, PaymentFailure failure, Instant now)` | `PAY-CMD-006` |
| `PAY-OP-007` | `authorizePosting` | `void authorizePosting(PostingInstructionIdentity instruction, Instant now)` | `PAY-CMD-007` |
| `PAY-OP-008` | `recordPostingOutcome` | `void recordPostingOutcome(PostingOutcomeSnapshot evidence, PaymentFailure failure, Instant now)` | `PAY-CMD-008` |
| `PAY-OP-009` | `resolvePostingOutcome` | `void resolvePostingOutcome(PostingOutcomeSnapshot evidence, PaymentFailure failure, Instant now)` | `PAY-CMD-009` |
| `PAY-OP-010` | `recordMatchedEndOfDayConfirmation` | `void recordMatchedEndOfDayConfirmation(EndOfDayConfirmationSnapshot evidence, PaymentFailure failure, Instant now)` | `PAY-CMD-010` |
| `PAY-OP-011` | `authorizeReversal` | `void authorizeReversal(ReversalInstructionIdentity instruction, ReversalAuthorizationEvidence authorization, Instant now)` | `PAY-CMD-011` |
| `PAY-OP-012` | `recordReversalOutcome` | `void recordReversalOutcome(ReversalSnapshot evidence, PaymentFailure failure, Instant now)` | `PAY-CMD-012` |
| `PAY-OP-013` | `resolveReversalOutcome` | `void resolveReversalOutcome(ReversalSnapshot evidence, PaymentFailure failure, Instant now)` | `PAY-CMD-013` |
| `PAY-OP-014` | `reject` | `void reject(PaymentFailure rejection, Instant finalizedAt)` | `PAY-CMD-014` |
| `PAY-OP-015` | `recordRecoverableFailure` | `void recordRecoverableFailure(PaymentFailure failure, Instant now)` | `PAY-CMD-015` |
| `PAY-OP-016` | `failWithoutFinancialEffect` | `void failWithoutFinancialEffect(PaymentFailure failure, Instant finalizedAt)` | `PAY-CMD-016` |
| `PAY-OP-017` | `reconstitute` | `static Payment reconstitute(PaymentState state)` | Persistence only |

### Operation rules

1. `receive` and `reconstitute` are static factories with different semantics.
2. `reconstitute` raises no fact, changes no time/version and accepts no
   application command.
3. Every mutation operation validates the current state before changing any
   field.
4. The operation selects its branch only from canonical typed evidence.
5. Favorable evidence clears a superseded recoverable failure when the
   lifecycle successfully resumes.
6. Terminal rejection/failure preserves its structured `PaymentFailure`.
7. A no-op identical evidence replay does not update `updatedAt`, version or
   domain facts.
8. Conflicting evidence raises a domain conflict without mutation.

## 9. External process operations excluded from Payment

The following are not aggregate commands or aggregate methods:

| Process request | Owner |
| --- | --- |
| Validate JWT/JWKS/signature | Security / Integration |
| Call customer/account verification | Customer / Integration |
| Call funds control | Accounting / Integration |
| Resolve protected Treasury account | Accounting / protected configuration |
| Submit or look up posting | Accounting / Integration |
| Deliver notification | Notification / Integration |
| Match, quarantine or look up TFJ | Accounting |
| Submit or look up reversal | Accounting / Integration |
| Publish or retry Outbox | Infrastructure / Integration |

Payment registers domain facts expressing that these processes are required.
Lot 2.6 assigns final event names and safe payloads.

**Decision:** `PAY-DEC-IA1-037`.

## 10. Negative outcome mapping

A canonical negative result is not mapped by generic text.

| Situation | Required category/evidence | Payment result |
| --- | --- | --- |
| Business/security rejection before financial submission | Conclusive `PaymentFailure` and no possible financial effect | `REJECTED` |
| Technical failure before financial submission | `TECHNICAL_FAILURE` and proven no effect | `FAILED` |
| Posting rejected with no effect | Posting snapshot + business failure | `REJECTED` |
| Posting technical failure with proven no effect | Posting snapshot + technical failure | `FAILED` |
| Posting/reversal unknown | `UNCERTAIN_EXTERNAL_OUTCOME` | Unknown non-terminal state |
| Partial financial effect | Posting evidence proving effect | `REVERSAL_REQUIRED` |
| TFJ `FAILED` | Matched TFJ evidence and recovery action | Reconciliation or reversal; never automatic `FAILED` |

**Decision:** `PAY-DEC-IA1-038`.

## 11. Final IA-1 Payment statuses

The target status set contains 17 values:

- `RECEIVED` — Authenticated canonical Payment intention is durably persisted.
- `AUTHORIZATION_CHECKING` — SIXPAY is awaiting or applying the canonical TRESOR PAY authorization decision.
- `BANKING_VERIFICATION_PENDING` — Authorization is approved and fresh customer/account verification is pending.
- `FUNDS_CONTROL_PENDING` — Banking identity/account verification is approved and exact funds control is pending.
- `TREASURY_ACCOUNT_RESOLUTION_PENDING` — Funds are approved and protected CUT/Treasury configuration resolution is pending.
- `APPROVED_FOR_POSTING` — All required favorable evidence exists and one posting may be authorized.
- `POSTING_PENDING` — The sole posting instruction is durable and may have reached Amplitude.
- `POSTING_OUTCOME_UNKNOWN` — Posting result is uncertain and only authoritative lookup/reconciliation is allowed.
- `DEBIT_CONFIRMED` — Debtor debit is confirmed while CUT credit is not confirmed complete.
- `POSTED_PENDING_TFJ` — Debit and CUT credit are confirmed; immediate result and TFJ tracking intents exist.
- `REVERSAL_REQUIRED` — A confirmed/reconciled effect requires explicit authorized reversal handling.
- `REVERSAL_PENDING` — An authorized reversal instruction is durable and awaiting a known result.
- `REVERSAL_OUTCOME_UNKNOWN` — Reversal result is uncertain and only authoritative lookup is allowed.
- `REJECTED` — Conclusive business/security rejection with no financial effect.
- `FAILED` — Technical processing ended with proven absence of financial effect.
- `TREASURY_INTEGRATED` — Uniquely matched Amplitude TFJ INTEGRATED evidence establishes finality.
- `REVERSED` — Authorized bank reversal is confirmed; original posting evidence remains.

Decisions:

- `NOTIFIED` is removed from the financial state machine.
- `CUT_CREDITED` and TFJ-wait sequencing are represented by
  `POSTED_PENDING_TFJ`.
- `ACCOUNTING_OUTCOME_UNKNOWN` is split into
  `POSTING_OUTCOME_UNKNOWN` and `REVERSAL_OUTCOME_UNKNOWN`.
- `DEBIT_CONFIRMED` is retained only for a proven partial posting fact.
- notification and TFJ tracking intents are registered atomically when a
  posting becomes `POSTED_PENDING_TFJ`.

**Decisions:** `PAY-DEC-IA1-035`, `PAY-DEC-IA1-036`.

## 12. Reconciled state machine

The final Lot 2.5 state machine is:

`documentation/ai/payment/PAYMENT_STATE_MACHINE.yaml`

It contains:

- 17 states;
- 4 terminal states;
- 38 legal transitions;
- no `NOTIFIED` state;
- separate posting/reversal unknown states;
- command and aggregate-operation traceability;
- invariant guards and replay rules.

The IA-1 file replaces the IA-0P transition semantics for the Payment model.

**Decision:** `PAY-DEC-IA1-039`.

## 13. Method-design decision

The target domain API uses explicit named methods and typed arguments. It does
not expose:

```text
setStatus
apply(Command)
transitionTo(arbitraryStatus)
updateFromExternalPayload
retryFinancialOperation
```

Application commands are not domain entities and are never persisted inside
the aggregate.

**Decision:** `PAY-DEC-IA1-040`.

## 14. Decisions closed in Lot 2.5

| Decision | Result |
| --- | --- |
| `PAY-DEC-IA1-033` | Commands remain application records; aggregate methods are named, typed and exception-based on invalid operations. |
| `PAY-DEC-IA1-034` | Common command metadata, deduplication and version modes are fixed. |
| `PAY-DEC-IA1-035` | Final target status set contains 17 unambiguous statuses. |
| `PAY-DEC-IA1-036` | Completed posting directly establishes `POSTED_PENDING_TFJ`; notification is not a state. |
| `PAY-DEC-IA1-037` | External processes are requested by facts and never executed by Payment. |
| `PAY-DEC-IA1-038` | `REJECTED`, `FAILED`, unknown and reversal-required mappings are evidence/category driven. |
| `PAY-DEC-IA1-039` | IA-1 state machine replaces IA-0P semantics with 38 traced transitions. |
| `PAY-DEC-IA1-040` | Generic status mutation and generic command dispatch inside the aggregate are forbidden. |

## 15. Deferred to following lots

- final event names, envelopes and safe payloads: Lot 2.6;
- freshness, authorization, matching and resolver policies: Lot 2.7;
- acceptance-scenario and complete cross-document reconciliation: Lot 2.8.

## 16. Exit checklist

- [ ] Every accepted Payment command has exact metadata and payload types.
- [ ] Every command maps to one named aggregate operation.
- [ ] Every operation defines allowed states and outcome branches.
- [ ] No external DTO or application command enters Payment.
- [ ] State transitions are guarded by invariant identifiers.
- [ ] One-posting and reversal identities are explicit.
- [ ] Negative outcomes map unambiguously.
- [ ] Notification is absent from the financial status set.
- [ ] State machine command/operation references are valid.
- [ ] Code generation remains forbidden.

## 17. Verdict

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
