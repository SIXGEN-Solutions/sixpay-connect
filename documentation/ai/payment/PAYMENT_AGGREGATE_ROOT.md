# SIXPAY CONNECT — Payment Aggregate Root

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.5 — Commands and Business Operations`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `COMMAND_AND_OPERATION_MODEL_PREPARED`  
> **Code generation:** **FORBIDDEN**

## 1. Aggregate decision

`Payment` remains the sole write Aggregate Root for one logical TRESOR PAY
payment intention.

Normative documents:

- `PAYMENT_VALUE_OBJECT_CATALOGUE.md`;
- `PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md`;
- `PAYMENT_INVARIANT_CATALOGUE.md`;
- `PAYMENT_COMMAND_CATALOGUE.md`;
- `PAYMENT_STATE_MACHINE.yaml`.

## 2. Aggregate state

### Required immutable creation state

- `PaymentId`;
- `PaymentSource`;
- `ExternalPaymentReference`;
- `ExternalSubscriptionReference`;
- `PublicPaymentReference`;
- `PaymentRequestIdentity`;
- `FinancialInstitutionCode`;
- `DebtorAccountReference`;
- requested `Money`;
- `TreasuryAllocationIntent`;
- `createdAt`.

### Mutable bounded lifecycle state

- current `PaymentStatus`;
- current accepted evidence snapshots;
- resolved `TreasuryAccountReference`;
- `PostingInstructionIdentity`;
- `BankPostingReference`;
- current `ReversalSnapshot`;
- current relevant `PaymentFailure`;
- `updatedAt`;
- `finalizedAt`;
- `businessVersion`.

The aggregate stores no command history, notification delivery, raw payload or
unbounded evidence history.

## 3. Target statuses

```text
RECEIVED
AUTHORIZATION_CHECKING
BANKING_VERIFICATION_PENDING
FUNDS_CONTROL_PENDING
TREASURY_ACCOUNT_RESOLUTION_PENDING
APPROVED_FOR_POSTING
POSTING_PENDING
POSTING_OUTCOME_UNKNOWN
DEBIT_CONFIRMED
POSTED_PENDING_TFJ
REVERSAL_REQUIRED
REVERSAL_PENDING
REVERSAL_OUTCOME_UNKNOWN
REJECTED
FAILED
TREASURY_INTEGRATED
REVERSED
```

Terminal states:

```text
REJECTED
FAILED
TREASURY_INTEGRATED
REVERSED
```

`NOTIFIED` is not a financial state.

## 4. Aggregate API

### Factories

```text
Payment.receive(...)
Payment.reconstitute(...)
```

`receive` creates `RECEIVED` and registers the received fact.

`reconstitute` restores state and raises no fact.

### Named mutation methods

```text
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

The exact conceptual signatures are normative in
`PAYMENT_COMMAND_CATALOGUE.md`.

There is no generic command dispatcher or public status setter.

## 5. Operation behavior

Every operation:

1. validates non-null typed arguments;
2. validates current status;
3. validates applicable `PAY-INV-*` rules;
4. detects identical evidence replay or conflict;
5. computes the complete next state before mutation;
6. applies all field changes atomically;
7. updates `updatedAt`;
8. increments `businessVersion` exactly once for a real mutation;
9. registers domain facts;
10. returns normally.

On violation it raises `PaymentDomainException` before mutation.

## 6. Posting instruction rule

`authorizePosting` persists one `PostingInstructionIdentity` before external
submission.

The same instruction may be safely re-observed or retried only with identical:

- instruction ID;
- bank idempotency key;
- instruction fingerprint;
- Payment amount/accounts/configuration.

A second logical instruction is rejected.

## 7. Posting outcome rule

`recordPostingOutcome` accepts direct canonical evidence from:

- `POSTING_PENDING`;
- `DEBIT_CONFIRMED`.

`resolvePostingOutcome` accepts authoritative lookup evidence only from
`POSTING_OUTCOME_UNKNOWN`.

The operation maps evidence to:

- `POSTED_PENDING_TFJ`;
- `DEBIT_CONFIRMED`;
- `POSTING_OUTCOME_UNKNOWN`;
- `REJECTED`;
- `FAILED`;
- `REVERSAL_REQUIRED`.

A result `UNKNOWN` never invokes posting again.

## 8. Notification and TFJ

When posting becomes `POSTED_PENDING_TFJ`, the same aggregate mutation
registers facts for:

- immediate `POSTED_PENDING_TFJ` result availability;
- TFJ tracking registration.

Notification delivery occurs independently.

`recordMatchedEndOfDayConfirmation` accepts only matched final evidence:

- `INTEGRATED` → `TREASURY_INTEGRATED`;
- `FAILED` + reversal required → `REVERSAL_REQUIRED`;
- `FAILED` + manual review → remains `POSTED_PENDING_TFJ`.

## 9. Reversal

`authorizeReversal` requires explicit authorization evidence and creates one
active `ReversalInstructionIdentity`.

Direct and lookup results are handled by separate named methods.

`UNKNOWN` produces `REVERSAL_OUTCOME_UNKNOWN`; it never causes blind
resubmission.

Confirmed reversal produces `REVERSED` while retaining original posting
identity and evidence.

## 10. Failure operations

`recordRecoverableFailure` is allowed only before financial submission and
keeps the current processing status.

`reject` creates terminal `REJECTED` for a conclusive business/security
rejection without possible financial effect.

`failWithoutFinancialEffect` creates terminal `FAILED` only for a technical
failure with proven absence of financial effect.

Posting and reversal results use their dedicated evidence operations rather
than these generic pre-financial methods.

## 11. Command boundary

Application commands do not enter the aggregate.

Command deduplication, expected-version checking, actor authorization and
command receipts remain in the application/persistence boundary.

The aggregate receives only typed domain arguments and a controlled `Instant`.

## 12. Applicable catalogues

- aggregate structural invariants: `PAY-AGG-001` to `PAY-AGG-014`;
- snapshot invariants: `PAY-SNAP-001` to `PAY-SNAP-018`;
- complete invariants: `PAY-INV-001` to `PAY-INV-076`;
- commands: `PAY-CMD-001` to `PAY-CMD-016`;
- operations: `PAY-OP-001` to `PAY-OP-017`;
- transitions: `PAY-TR-001` to `PAY-TR-038`;
- Lot 2.5 decisions: `PAY-DEC-IA1-033` to `PAY-DEC-IA1-040`.

## 13. Deferred scope

- final event names and payloads: Lot 2.6;
- policies and Domain Services: Lot 2.7;
- full acceptance and model validation: Lot 2.8.

## 14. Verdict

```text
AGGREGATE ROOT: PREPARED
VALUE OBJECTS: PREPARED
SNAPSHOTS: PREPARED
INVARIANTS: PREPARED
COMMANDS AND OPERATIONS: PREPARED
STATE MACHINE: RECONCILED
CODE GENERATION: FORBIDDEN
```
