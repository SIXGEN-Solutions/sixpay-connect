# SIXPAY CONNECT — Payment Aggregate Root

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Lot:** `2.1 — Aggregate Root Payment`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `DRAFT_PENDING_VALIDATION`  
> **Code generation:** **FORBIDDEN**

## 1. Purpose

This document defines the responsibility, identity, boundary, conceptual state,
creation rules and reconstitution rules of the `Payment` Aggregate Root.

It intentionally does not finalize:

- detailed Value Object formats;
- snapshot structures;
- the complete invariant catalogue;
- command signatures;
- state-machine transitions;
- domain-event payloads;
- policies and Domain Services.

Those topics are assigned to Lots 2.2 through 2.7.

## 2. Aggregate decision

`Payment` is the sole Aggregate Root of the Payment write model for the MVP.

It represents SIXPAY's durable treatment of one unique payment intention
received from TRESOR PAY.

One `Payment` corresponds to one logical TRESOR PAY payment intention, even
when:

- the inbound request is replayed;
- technical recovery is required;
- a posting outcome is uncertain;
- notifications are redelivered;
- a reversal is later requested.

A replay, recovery, notification retry or reversal never creates a second
`Payment` for the original intention.

**Sources:** `PAY-BASE-001`, `PAY-BASE-002`, `PAY-BOUND-001` to
`PAY-BOUND-012`, `PAY-CONTRACT-001`.

## 3. Business responsibility

`Payment` is responsible for:

1. identifying the unique payment intention treated by SIXPAY;
2. retaining the canonical references needed to correlate that intention;
3. retaining the requested amount and currency;
4. owning the current SIXPAY Payment lifecycle state;
5. deciding whether a named lifecycle transition is legal;
6. recording minimized immutable evidence used by its decisions;
7. recording the current relevant failure or rejection;
8. preventing a second financial effect for the same logical Payment;
9. distinguishing request acceptance, banking approval, posting outcome,
   CUT credit and TFJ finality;
10. recording the need for notification through domain events or notification
    intentions;
11. preserving business version and temporal consistency;
12. raising domain events for accepted business facts.

`Payment` owns the interpretation of canonical external results. It does not
own the external process that produced them.

## 4. Explicit non-responsibilities

`Payment` does not own or execute:

- TRESOR PAY Subscription lifecycle;
- JWT, Subscription Key, credentials or JWKS;
- customer or account master data;
- current account balance;
- protected CUT configuration;
- HTTP, REST, Kafka or internal messaging transport;
- Amplitude client execution;
- retry, timeout or circuit-breaker mechanics;
- notification delivery attempts, retry or DLQ;
- generic Outbox table or relay implementation;
- unmatched TFJ result handling or quarantine;
- query projections and full operational timeline;
- unbounded audit or transition history;
- reversal transport execution;
- receipt generation.

These responsibilities remain with TRESOR PAY, Security, Customer,
Integration, Accounting, Notification, Reporting or infrastructure according
to `PAYMENT_DOMAIN_BOUNDARIES.md`.

## 5. Aggregate identity and references

### 5.1 Aggregate identity

`PaymentId` is the technical identity of the aggregate inside SIXPAY.

It is distinct from:

- `ExternalPaymentReference`, the TRESOR PAY `endToEndId` /
  `tresorPayPaymentReference`;
- the public SIXPAY Payment reference exposed to operators and external
  consumers;
- an idempotency key;
- a correlation identifier;
- a bank posting reference.

The final formats and generation policies are defined in Lot 2.2.

### 5.2 Business uniqueness

The aggregate itself protects consistency after it has been loaded, but global
uniqueness of the TRESOR PAY payment intention requires an application and
persistence boundary.

Conceptually:

```text
PaymentSource + ExternalPaymentReference
```

forms the business uniqueness scope.

For the MVP, `PaymentSource` is fixed to `TRESOR_PAY`. The source component is
still retained conceptually so the uniqueness rule is explicit and does not
silently change if another source is added later.

The exact persistence constraint is deferred to implementation planning.

**Decision:** `PAY-DEC-IA1-002 — SOURCE_SCOPED_EXTERNAL_PAYMENT_UNIQUENESS`.

## 6. Conceptual aggregate state

The Aggregate Root may contain the following conceptual state.

### 6.1 Required from creation

| Attribute | Conceptual type | Purpose |
| --- | --- | --- |
| `id` | `PaymentId` | SIXPAY aggregate identity |
| `source` | `PaymentSource` | Origin of the logical payment intention |
| `externalPaymentReference` | `ExternalPaymentReference` | TRESOR PAY unique end-to-end reference |
| `externalSubscriptionReference` | `ExternalSubscriptionReference` | External traceability only |
| `publicPaymentReference` | `PublicPaymentReference` | Stable SIXPAY-facing business reference |
| `requestIdentity` | `PaymentRequestIdentity` | Idempotency key, fingerprint and correlation identity |
| `financialInstitution` | `FinancialInstitutionCode` | Bank context |
| `debtorAccount` | `DebtorAccountReference` | Protected debtor-account identity |
| `requestedAmount` | `Money` | Positive amount and currency |
| `treasuryAllocationIntent` | bounded immutable allocation intent or reference | Requested Treasury distribution before protected resolution |
| `status` | `PaymentStatus` | Current SIXPAY lifecycle state |
| `createdAt` | `Instant` | Durable reception time |
| `updatedAt` | `Instant` | Last accepted aggregate mutation time |
| `businessVersion` | non-negative monotonic version | Ordering and optimistic consistency |

Detailed allocation modelling is deferred to Lot 2.2. The aggregate must not
store unbounded beneficiary collections.

### 6.2 Optional lifecycle evidence

| Attribute | Conceptual type | Appears when |
| --- | --- | --- |
| `authorizationEvidence` | `AuthorizationEvidenceSnapshot` | Authorization result has been accepted |
| `bankingVerificationEvidence` | `BankingVerificationSnapshot` | Banking verification result has been accepted |
| `fundsControlEvidence` | `FundsControlSnapshot` | Fresh funds decision has been accepted |
| `resolvedTreasuryAccount` | `TreasuryAccountReference` | Protected CUT configuration has been resolved and validated |
| `postingOutcome` | `PostingOutcomeSnapshot` | Posting command has a canonical known or uncertain outcome |
| `bankPostingReference` | `BankPostingReference` | Principal reference is known; mandatory after confirmed posting |
| `tfjConfirmation` | `EndOfDayConfirmationSnapshot` | A matched canonical TFJ result has been accepted |
| `reversalOutcome` | `ReversalSnapshot` | Reversal lifecycle has a canonical outcome |
| `failure` | `PaymentFailure` | A currently relevant failure or rejection exists |
| `finalizedAt` | `Instant` | A terminal business state is reached |

The detailed snapshot and evidence structures are deferred to Lot 2.3.

### 6.3 State intentionally not retained

The aggregate must not retain:

- raw TRESOR PAY or Amplitude payloads;
- credentials or tokens;
- clear account numbers solely for display;
- every integration attempt;
- every notification attempt;
- every domain event ever raised;
- every failure ever observed;
- unmatched TFJ records;
- investigation comments;
- operator attachments;
- delivery acknowledgements;
- read-model formatting fields.

## 7. Aggregate lifecycle principles

### 7.1 Initial creation

The aggregate is created only after transport authentication and minimum
canonical request validation have succeeded sufficiently to establish a
Payment intention.

The conceptual factory is:

```text
Payment.receive(ReceivePaymentInput, PaymentIdentity, Instant)
```

The final command and parameter types are deferred to Lots 2.2 and 2.5.

Creation must:

1. require all mandatory creation concepts;
2. validate the amount is positive;
3. initialize status to `RECEIVED`;
4. initialize `createdAt` and `updatedAt` to the same instant;
5. initialize business version according to the repository convention;
6. contain no external call;
7. raise one Payment-received domain fact;
8. leave authorization, banking, funds, posting, TFJ and reversal evidence
   absent.

An unauthenticated request does not create a Payment. It is a Security /
Integration incident.

A request authenticated successfully but rejected only after durable business
admission may create a Payment that later reaches a definitive rejection.

### 7.2 Reconstitution

Persistence reconstitution must use a distinct conceptual operation:

```text
Payment.reconstitute(PaymentState)
```

Reconstitution:

- restores already validated persisted state;
- performs structural consistency validation;
- raises no domain event;
- performs no lifecycle transition;
- does not call external systems;
- does not regenerate references;
- does not change timestamps or version.

The persistence adapter must not use public business transition methods to
rebuild an aggregate.

### 7.3 Mutation

Every aggregate mutation must:

1. occur through a named business operation;
2. validate the current status and required evidence;
3. reject invalid transitions before mutation;
4. update all related state atomically;
5. update `updatedAt`;
6. increment or prepare the business version according to the persistence
   convention;
7. register the corresponding domain event;
8. preserve the original identity, source, external reference, public reference,
   amount and creation time.

There is no public generic status setter.

## 8. Transaction boundary

One application transaction affecting Payment must:

- load or create at most one `Payment` Aggregate Root;
- execute one use-case decision;
- persist the resulting aggregate state;
- append the immutable Payment audit entry;
- append the domain Outbox intent;
- commit these changes atomically.

No network operation belongs inside the aggregate mutation transaction.

External calls are orchestrated outside the aggregate through application
ports. Their canonical results are later supplied to named aggregate methods.

**Sources:** `PAY-BASE-024`, `PAY-BASE-025`, `PAY-BOUND-004`,
Golden Partner transaction convention.

## 9. Domain-event handling inside the aggregate

`Payment` extends the shared-kernel `AggregateRoot<PaymentId>` concept and uses
its in-memory domain-event registration mechanism.

The aggregate:

- registers a domain event only after a successful state mutation;
- exposes pending events through the shared-kernel release mechanism;
- does not publish directly to Kafka or an HTTP endpoint;
- does not create Outbox rows itself;
- does not retain a permanent collection of notification intentions.

The application/infrastructure layer maps pending domain events to audit and
Outbox records within the same transaction.

This follows the Golden Partner pattern without copying Partner business
semantics.

## 10. Failure ownership

For Lot 2.1, `Payment` owns at most one optional **current relevant**
`PaymentFailure`.

It does not retain an internal unbounded failure history.

The full failure history belongs to append-only audit and reporting
projections.

A new accepted transition may:

- create a current failure;
- replace an earlier non-terminal technical failure with a newer relevant
  failure;
- clear a recoverable technical failure when the workflow successfully
  resumes;
- preserve a definitive rejection failure in a terminal rejected state.

The precise `PaymentFailure` structure is defined in Lot 2.2 and its transition
rules in Lots 2.4 and 2.5.

**Decision:** `PAY-DEC-IA1-003 — CURRENT_FAILURE_IN_AGGREGATE_HISTORY_IN_AUDIT`.

## 11. Notification ownership

Payment owns notification intent as a domain fact, not as a delivery process.

A successful business transition may raise a notification-requested domain
event. The aggregate does not store:

- delivery status;
- attempt counter;
- retry schedule;
- HTTP response;
- DLQ state.

Consequently, notification delivery does not create a Payment financial status
transition.

This confirms the Lot 1 recommendation that `NOTIFIED` is not a main financial
status. Final removal or reclassification of that candidate state is completed
with the state machine work.

**Decision:** `PAY-DEC-IA1-004 — NOTIFICATION_AS_DOMAIN_INTENT_NOT_FINANCIAL_STATE`.

## 12. Posting and reversal identity

The original posting identity remains part of Payment after reversal.

A reversal:

- has its own identity and evidence;
- never replaces or deletes `BankPostingReference`;
- never creates a second Payment;
- is represented as a later lifecycle process on the same aggregate.

`BankPostingReference` follows `PAY-DEC-IA1-001`:

- mandatory principal reference after confirmed posting;
- optional debit-leg reference;
- optional CUT-credit-leg reference.

## 13. Aggregate methods — responsibility map

The following named operations are reserved conceptually. Final signatures,
guards and events are deferred.

| Responsibility | Candidate named operation |
| --- | --- |
| Create Payment | `receive` |
| Start authorization evaluation | `startAuthorizationChecking` |
| Record authorization approval | `recordAuthorizationApproved` |
| Record authorization rejection | `recordAuthorizationRejected` |
| Start banking verification | `startBankingChecking` |
| Record banking verification | `recordBankingVerification` |
| Record funds-control result | `recordFundsControl` |
| Approve for posting | `approveForPosting` |
| Start posting | `startPosting` |
| Record posting outcome | `recordPostingOutcome` |
| Record uncertain outcome resolution | `resolvePostingOutcome` |
| Record matched TFJ result | `recordMatchedEndOfDayConfirmation` |
| Record technical failure without proven effect | `recordTechnicalFailure` |
| Reject definitively | `reject` |
| Request reversal | `requestReversal` |
| Record reversal outcome | `recordReversalOutcome` |

This table allocates responsibility only. Lot 2.5 defines final commands and
method contracts.

## 14. Aggregate-level structural invariants

These invariants are fixed at Lot 2.1:

| ID | Invariant |
| --- | --- |
| `PAY-AGG-001` | A Payment has exactly one immutable `PaymentId`. |
| `PAY-AGG-002` | A Payment has exactly one immutable source-scoped external payment reference. |
| `PAY-AGG-003` | A Payment represents one logical payment intention for its entire lifecycle. |
| `PAY-AGG-004` | Aggregate identity, source, external reference, public reference, original amount and creation time never change. |
| `PAY-AGG-005` | Aggregate status changes only through named operations. |
| `PAY-AGG-006` | No external transport or persistence dependency enters the domain model. |
| `PAY-AGG-007` | Raw credentials and external payloads never enter aggregate state or events. |
| `PAY-AGG-008` | Unbounded histories and delivery attempts remain outside the aggregate. |
| `PAY-AGG-009` | A reversal preserves the original posting identity and Payment identity. |
| `PAY-AGG-010` | Reconstitution raises no event and applies no business transition. |
| `PAY-AGG-011` | Every successful mutation updates temporal/version consistency and raises the corresponding domain fact. |
| `PAY-AGG-012` | An invalid operation produces no partial aggregate mutation. |
| `PAY-AGG-013` | Notification delivery status never determines financial status. |
| `PAY-AGG-014` | One Payment cannot authorize more than one independent posting for the original intention. |

Detailed business invariants are completed in Lot 2.4.

## 15. Conceptual dependency rule

The future domain implementation may depend on:

- Java standard-library types;
- `com.sixpay.sharedkernel.domain.model.AggregateRoot`;
- shared-kernel domain concepts explicitly approved for reuse;
- Payment-local Value Objects, snapshots, failures and domain events.

It must not depend on:

- Spring;
- Jakarta Persistence;
- Hibernate;
- Jackson;
- Kafka;
- HTTP clients;
- another business-domain module;
- external DTO classes;
- database entities.

## 16. Decisions closed in Lot 2.1

| Decision ID | Decision |
| --- | --- |
| `PAY-DEC-IA1-002` | External Payment uniqueness is source-scoped; MVP source is fixed to TRESOR PAY. |
| `PAY-DEC-IA1-003` | Aggregate retains only current relevant `PaymentFailure`; history is append-only audit/reporting. |
| `PAY-DEC-IA1-004` | Notification is a domain intent/event, not a main financial state or stored delivery process. |
| `PAY-DEC-IA1-005` | Aggregate reconstitution is distinct from creation and raises no events. |
| `PAY-DEC-IA1-006` | Reversal remains on the original Payment and preserves original posting identity. |

## 17. Deferred decisions

| Deferred to | Subject |
| --- | --- |
| Lot 2.2 | Exact identifier formats, normalization, account protection and failure types |
| Lot 2.3 | Snapshot structures and evidence minimization |
| Lot 2.4 | Complete invariant catalogue |
| Lot 2.5 | Final aggregate methods, commands and guards |
| Lot 2.6 | Domain-event names and payloads |
| Lot 2.7 | Policies and Domain Services |
| Lot 2.8 | Cross-document validation and generation readiness |

## 18. Exit criterion

Lot 2.1 is complete when:

- `Payment` is confirmed as the sole write Aggregate Root;
- its responsibilities and exclusions are explicit;
- its conceptual state is bounded;
- creation and reconstitution are distinct;
- global uniqueness responsibility is separated from aggregate consistency;
- failure and notification ownership are resolved;
- transaction and event-registration boundaries are explicit;
- no Value Object implementation detail is invented prematurely;
- no code generation is authorized.

## 19. Verdict

```text
IA-1 LOT 2.1 PAYMENT AGGREGATE ROOT PREPARED
STATUS: DRAFT_PENDING_VALIDATION
NEXT: LOT 2.2 — IDENTIFIERS AND VALUE OBJECTS
CODE GENERATION: FORBIDDEN
```
