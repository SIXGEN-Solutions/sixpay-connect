# SIXPAY CONNECT — Payment Domain Event Catalogue

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.8 — Final Model Validation`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `FINAL_VALIDATED`  
> **Code generation:** **FORBIDDEN_PENDING_EXPLICIT_APPROVAL**

## 1. Purpose

This document defines the immutable domain events registered by the `Payment`
Aggregate Root after successful mutations.

It specifies:

- the common Payment event contract;
- exact stable event names;
- payload minimization and confidentiality;
- producers, consumers and side-effect ownership;
- ordering, versioning, replay and deduplication;
- the mapping from Lot 2.5 domain fact kinds to event types.

Supporting modules own their own process events. In particular,
`PaymentNotificationDelivered` is not a Payment domain event.

## 2. Domain event versus integration envelope

A conceptual Payment event follows:

```text
PaymentDomainEvent extends DomainEvent
├── UUID eventId
├── PaymentId paymentId
├── PublicPaymentReference paymentReference
├── CorrelationId correlationId
├── long aggregateVersion
├── int eventSequence
├── UUID? causationId
└── Instant occurredAt
```

The shared-kernel `DomainEvent` supplies the base `eventId`, `occurredAt` and
logical `eventType` contract.

The Outbox mapper converts the domain event into the existing
`IntegrationEventEnvelope`:

```text
eventId
eventType
schemaVersion
aggregateType = PAYMENT
aggregateId = PaymentId UUID
correlationId
occurredAt
payload = explicit safe JSON
```

The domain record and integration envelope are distinct representations.

## 3. Multiple events from one mutation

One aggregate operation may register multiple facts.

All events from that mutation:

- carry the same resulting `aggregateVersion`;
- use `eventSequence = 1..n` in registration order;
- commit atomically with Payment state and audit;
- are published with `aggregateId` as the partition key.

No-op replay, rejected operation, conflict, stale command and reconstitution
produce no event.

## 4. Event roles

| Role | Meaning |
| --- | --- |
| `LIFECYCLE_FACT` | Payment state or processing fact |
| `EVIDENCE_FACT` | Canonical non-financial evidence accepted |
| `FINANCIAL_EVIDENCE_FACT` | Canonical posting or reversal evidence accepted |
| `FINANCIAL_FACT` | Confirmed financial lifecycle fact |
| `TERMINAL_FACT` | Terminal non-financial outcome |
| `TERMINAL_FINANCIAL_FACT` | Terminal financial outcome |
| `FINANCIAL_AUTHORIZATION_FACT` | Explicit durable authorization of one financial instruction |
| `PROCESS_REQUEST` | Durable fact requesting a supporting process after commit |
| `RESULT_INTENT` | Durable result availability consumed only by Notification |
| `LIFECYCLE_AND_PROCESS_REQUEST` | State fact that also opens an operational workflow |
| `TREASURY_EVIDENCE_FACT` | Uniquely matched TFJ evidence accepted |

## 5. Catalogue summary

| ID | Event type | Fact kind | Role |
| --- | --- | --- | --- |
| `PAY-EVT-001` | `PaymentReceived` | `PAYMENT_RECEIVED` | `LIFECYCLE_FACT` |
| `PAY-EVT-002` | `PaymentAuthorizationCheckingStarted` | `AUTHORIZATION_CHECKING_STARTED` | `PROCESS_REQUEST` |
| `PAY-EVT-003` | `PaymentAuthorizationDecisionRecorded` | `AUTHORIZATION_DECISION_RECORDED` | `EVIDENCE_FACT` |
| `PAY-EVT-004` | `PaymentBankingVerificationRequested` | `BANKING_VERIFICATION_REQUESTED` | `PROCESS_REQUEST` |
| `PAY-EVT-005` | `PaymentRejected` | `PAYMENT_REJECTED` | `TERMINAL_FACT` |
| `PAY-EVT-006` | `PaymentImmediateResultAvailable` | `IMMEDIATE_RESULT_AVAILABLE` | `RESULT_INTENT` |
| `PAY-EVT-007` | `PaymentBankingVerificationRecorded` | `BANKING_VERIFICATION_RECORDED` | `EVIDENCE_FACT` |
| `PAY-EVT-008` | `PaymentFundsControlRequested` | `FUNDS_CONTROL_REQUESTED` | `PROCESS_REQUEST` |
| `PAY-EVT-009` | `PaymentProcessingDeferred` | `PAYMENT_PROCESSING_DEFERRED` | `LIFECYCLE_FACT` |
| `PAY-EVT-010` | `PaymentFundsControlRecorded` | `FUNDS_CONTROL_RECORDED` | `EVIDENCE_FACT` |
| `PAY-EVT-011` | `PaymentTreasuryAccountResolutionRequested` | `TREASURY_ACCOUNT_RESOLUTION_REQUESTED` | `PROCESS_REQUEST` |
| `PAY-EVT-012` | `PaymentTreasuryAccountResolutionRecorded` | `TREASURY_ACCOUNT_RESOLUTION_RECORDED` | `EVIDENCE_FACT` |
| `PAY-EVT-013` | `PaymentApprovedForPosting` | `PAYMENT_APPROVED_FOR_POSTING` | `LIFECYCLE_FACT` |
| `PAY-EVT-014` | `PaymentPostingAuthorized` | `POSTING_AUTHORIZED` | `FINANCIAL_AUTHORIZATION_FACT` |
| `PAY-EVT-015` | `PaymentPostingRequested` | `POSTING_REQUESTED` | `PROCESS_REQUEST` |
| `PAY-EVT-016` | `PaymentPostingOutcomeRecorded` | `POSTING_OUTCOME_RECORDED` | `FINANCIAL_EVIDENCE_FACT` |
| `PAY-EVT-017` | `PaymentEndOfDayTrackingRequested` | `TFJ_TRACKING_REQUESTED` | `PROCESS_REQUEST` |
| `PAY-EVT-018` | `PaymentDebitConfirmed` | `DEBIT_CONFIRMED` | `FINANCIAL_FACT` |
| `PAY-EVT-019` | `PaymentPostingOutcomeLookupRequested` | `POSTING_OUTCOME_LOOKUP_REQUESTED` | `PROCESS_REQUEST` |
| `PAY-EVT-020` | `PaymentReversalRequired` | `REVERSAL_REVIEW_REQUESTED` | `LIFECYCLE_AND_PROCESS_REQUEST` |
| `PAY-EVT-021` | `PaymentPostingOutcomeResolved` | `POSTING_OUTCOME_RESOLVED` | `FINANCIAL_EVIDENCE_FACT` |
| `PAY-EVT-022` | `PaymentEndOfDayConfirmationRecorded` | `TFJ_CONFIRMATION_RECORDED` | `TREASURY_EVIDENCE_FACT` |
| `PAY-EVT-023` | `TreasuryIntegrationConfirmed` | `TREASURY_INTEGRATION_CONFIRMED` | `TERMINAL_FINANCIAL_FACT` |
| `PAY-EVT-024` | `PaymentFinalResultAvailable` | `FINAL_RESULT_AVAILABLE` | `RESULT_INTENT` |
| `PAY-EVT-025` | `PaymentTreasuryReconciliationRequired` | `TREASURY_RECONCILIATION_REQUIRED` | `PROCESS_REQUEST` |
| `PAY-EVT-026` | `PaymentReversalAuthorized` | `REVERSAL_AUTHORIZED` | `FINANCIAL_AUTHORIZATION_FACT` |
| `PAY-EVT-027` | `PaymentReversalRequested` | `REVERSAL_REQUESTED` | `PROCESS_REQUEST` |
| `PAY-EVT-028` | `PaymentReversalOutcomeRecorded` | `REVERSAL_OUTCOME_RECORDED` | `FINANCIAL_EVIDENCE_FACT` |
| `PAY-EVT-029` | `PaymentReversalResultAvailable` | `REVERSAL_RESULT_AVAILABLE` | `RESULT_INTENT` |
| `PAY-EVT-030` | `PaymentReversalOutcomeLookupRequested` | `REVERSAL_OUTCOME_LOOKUP_REQUESTED` | `PROCESS_REQUEST` |
| `PAY-EVT-031` | `PaymentReversalOutcomeResolved` | `REVERSAL_OUTCOME_RESOLVED` | `FINANCIAL_EVIDENCE_FACT` |
| `PAY-EVT-032` | `PaymentFailedWithoutFinancialEffect` | `PAYMENT_FAILED_WITHOUT_FINANCIAL_EFFECT` | `TERMINAL_FACT` |
| `PAY-EVT-033` | `PaymentReversed` | `PAYMENT_REVERSED` | `TERMINAL_FINANCIAL_FACT` |

## 6. Detailed events

### `PAY-EVT-001` — `PaymentReceived`

A canonical authenticated Payment intention has been durably created.

- **Fact kind:** `PAYMENT_RECEIVED`
- **Role:** `LIFECYCLE_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-001`
- **Operations:** `PAY-OP-001`
- **Transitions:** aggregate creation

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `externalPaymentReference` | `ExternalPaymentReference` | `yes` | `BUSINESS_REFERENCE` |
| `source` | `PaymentSource` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `financialInstitutionCode` | `FinancialInstitutionCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `requestedAmount` | `MoneyPayload` | `yes` | `RESTRICTED_FINANCIAL` |
| `maskedDebtorAccountReference` | `string` | `yes` | `RESTRICTED_DISPLAY` |
| `receivedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `customer` — Create or update the initial ObservedCustomer projection from minimized Payment facts. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- ExternalSubscriptionReference unless an approved consumer contract requires it.
- Clear debtor account.
- Inbound headers or raw request.
- Authorization token or Subscription Key.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-002` — `PaymentAuthorizationCheckingStarted`

Payment has durably requested authorization-evidence evaluation.

- **Fact kind:** `AUTHORIZATION_CHECKING_STARTED`
- **Role:** `PROCESS_REQUEST`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-002`
- **Operations:** `PAY-OP-002`
- **Transitions:** `PAY-TR-001`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `startedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `security` — Resolve and validate canonical TRESOR PAY authorization evidence. Side effect: `IDEMPOTENT_SECURITY_VALIDATION`; idempotency: `eventId`.
- `integration` — Coordinate local token/JWKS validation without exposing credentials to Payment. Side effect: `IDEMPOTENT_SECURITY_VALIDATION`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `REVALIDATION_ALLOWED_BY_EVENT_ID_NO_FINANCIAL_EFFECT`.

### `PAY-EVT-003` — `PaymentAuthorizationDecisionRecorded`

Payment has accepted a canonical authorization decision.

- **Fact kind:** `AUTHORIZATION_DECISION_RECORDED`
- **Role:** `EVIDENCE_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-003`
- **Operations:** `PAY-OP-003`
- **Transitions:** `PAY-TR-002`, `PAY-TR-003`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `outcome` | `APPROVED|REJECTED` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `authorizationEvidenceReference` | `AuthorizationEvidenceReference` | `yes` | `RESTRICTED_SECURITY_EVIDENCE` |
| `evidenceFingerprint` | `EvidenceFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `rejectionCode` | `FailureCode` | `no` | `INTERNAL_NON_SENSITIVE` |
| `acceptedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Raw JWT.
- Raw jti.
- Raw claims.
- Signature bytes.
- JWKS document.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-004` — `PaymentBankingVerificationRequested`

Payment has durably requested fresh banking customer/account verification.

- **Fact kind:** `BANKING_VERIFICATION_REQUESTED`
- **Role:** `PROCESS_REQUEST`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-003`
- **Operations:** `PAY-OP-003`
- **Transitions:** `PAY-TR-002`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `financialInstitutionCode` | `FinancialInstitutionCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `debtorAccountBindingFingerprint` | `AccountBindingFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `requestedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `customer` — Execute or reuse the canonical fresh customer/account verification workflow. Side effect: `IDEMPOTENT_READ_WORKFLOW`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Clear account.
- Account integration token.
- NIU or customer identity values.
- Requested KYC payload.

**Replay:** `SAFE_READ_RETRY_KEYED_BY_EVENT_ID_AND_PAYMENT_VERSION`.

### `PAY-EVT-005` — `PaymentRejected`

Payment has reached terminal REJECTED with proven absence of financial effect.

- **Fact kind:** `PAYMENT_REJECTED`
- **Role:** `TERMINAL_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-003`, `PAY-CMD-004`, `PAY-CMD-005`, `PAY-CMD-006`, `PAY-CMD-008`, `PAY-CMD-009`, `PAY-CMD-014`
- **Operations:** `PAY-OP-003`, `PAY-OP-004`, `PAY-OP-005`, `PAY-OP-006`, `PAY-OP-008`, `PAY-OP-009`, `PAY-OP-014`
- **Transitions:** `PAY-TR-003`, `PAY-TR-005`, `PAY-TR-008`, `PAY-TR-012`, `PAY-TR-016`, `PAY-TR-024`, `PAY-TR-036`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `failureCode` | `FailureCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `failureCategory` | `FailureCategory` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `failureStage` | `FailureStage` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `finalizedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `customer` — Update ObservedCustomer rejection and decision counters. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Human free-form provider error.
- Raw KYC or account data.
- Stack trace.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-006` — `PaymentImmediateResultAvailable`

A non-final or immediate Payment result is durably available for delivery.

- **Fact kind:** `IMMEDIATE_RESULT_AVAILABLE`
- **Role:** `RESULT_INTENT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-003`, `PAY-CMD-004`, `PAY-CMD-005`, `PAY-CMD-006`, `PAY-CMD-008`, `PAY-CMD-009`, `PAY-CMD-010`, `PAY-CMD-014`, `PAY-CMD-015`, `PAY-CMD-016`
- **Operations:** `PAY-OP-003`, `PAY-OP-004`, `PAY-OP-005`, `PAY-OP-006`, `PAY-OP-008`, `PAY-OP-009`, `PAY-OP-010`, `PAY-OP-014`, `PAY-OP-015`, `PAY-OP-016`
- **Transitions:** `PAY-TR-003`, `PAY-TR-005`, `PAY-TR-006`, `PAY-TR-008`, `PAY-TR-010`, `PAY-TR-012`, `PAY-TR-013`, `PAY-TR-014`, `PAY-TR-015`, `PAY-TR-016`, `PAY-TR-017`, `PAY-TR-018`, `PAY-TR-019`, `PAY-TR-020`, `PAY-TR-021`, `PAY-TR-022`, `PAY-TR-023`, `PAY-TR-024`, `PAY-TR-025`, `PAY-TR-026`, `PAY-TR-028`, `PAY-TR-036`, `PAY-TR-037`, `PAY-TR-038`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `externalPaymentReference` | `ExternalPaymentReference` | `yes` | `BUSINESS_REFERENCE` |
| `resultType` | `REJECTED|FAILED|PROCESSING|POSTED_PENDING_TFJ|REVERSAL_REQUIRED` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `failureCode` | `FailureCode` | `no` | `INTERNAL_NON_SENSITIVE` |
| `principalPostingReference` | `BankPostingReference.principal` | `no` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `businessDate` | `LocalDate` | `no` | `INTERNAL_NON_SENSITIVE` |
| `availableAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `notification` — Create or reuse the immediate TRESOR PAY NotificationDelivery. Side effect: `TRESOR_PAY_NOTIFICATION`; idempotency: `sourceEventId+IMMEDIATE`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Customer identity.
- Clear account.
- Provider diagnostics.
- Full PaymentFailure.safeMessage by default.

**Replay:** `NOTIFICATION_REUSES_SOURCE_EVENT_ID_AND_PHASE`.

### `PAY-EVT-007` — `PaymentBankingVerificationRecorded`

Payment has accepted a canonical banking verification result.

- **Fact kind:** `BANKING_VERIFICATION_RECORDED`
- **Role:** `EVIDENCE_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-004`
- **Operations:** `PAY-OP-004`
- **Transitions:** `PAY-TR-004`, `PAY-TR-005`, `PAY-TR-006`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `verificationId` | `BankingVerificationId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `outcome` | `VERIFIED|REJECTED|INDETERMINATE` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `checkResults` | `bounded list<SafeCheckResult>` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `evidenceFingerprint` | `EvidenceFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `acceptedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `customer` — Update ObservedCustomer with minimized observed banking outcomes. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- NIU.
- Customer name/contact.
- KYC values.
- Raw customer/account reference.
- Available balance.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-008` — `PaymentFundsControlRequested`

Payment has durably requested exact funds and execution checks.

- **Fact kind:** `FUNDS_CONTROL_REQUESTED`
- **Role:** `PROCESS_REQUEST`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-004`
- **Operations:** `PAY-OP-004`
- **Transitions:** `PAY-TR-004`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `financialInstitutionCode` | `FinancialInstitutionCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `requestedAmount` | `MoneyPayload` | `yes` | `RESTRICTED_FINANCIAL` |
| `debtorAccountBindingFingerprint` | `AccountBindingFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `requestedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Execute or reuse the exact amount/account read-only funds control. Side effect: `IDEMPOTENT_READ_WORKFLOW`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Clear account.
- Account integration token.
- Current or available balance.

**Replay:** `SAFE_READ_RETRY_KEYED_BY_EVENT_ID_AND_PAYMENT_VERSION`.

### `PAY-EVT-009` — `PaymentProcessingDeferred`

Payment processing remains non-terminal while controlled recovery or operator action is required.

- **Fact kind:** `PAYMENT_PROCESSING_DEFERRED`
- **Role:** `LIFECYCLE_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-004`, `PAY-CMD-005`, `PAY-CMD-015`
- **Operations:** `PAY-OP-004`, `PAY-OP-005`, `PAY-OP-015`
- **Transitions:** `PAY-TR-006`, `PAY-TR-010`, `PAY-TR-037`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `failureCode` | `FailureCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `failureCategory` | `FailureCategory` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `failureStage` | `FailureStage` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `retryDisposition` | `RetryDisposition` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `deferredAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Stack trace.
- Endpoint.
- Raw provider error.
- Credential.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-010` — `PaymentFundsControlRecorded`

Payment has accepted a canonical funds-control result for its exact amount and account.

- **Fact kind:** `FUNDS_CONTROL_RECORDED`
- **Role:** `EVIDENCE_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-005`
- **Operations:** `PAY-OP-005`
- **Transitions:** `PAY-TR-007`, `PAY-TR-008`, `PAY-TR-010`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `verificationReference` | `FundsVerificationReference` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `outcome` | `VERIFIED|REJECTED|INDETERMINATE` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `checkResults` | `bounded list<SafeCheckResult>` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `validUntil` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `evidenceFingerprint` | `EvidenceFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `acceptedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Available amount.
- Ledger balance.
- Clear account.
- Raw Amplitude response.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-011` — `PaymentTreasuryAccountResolutionRequested`

Payment has durably requested protected Treasury-account resolution.

- **Fact kind:** `TREASURY_ACCOUNT_RESOLUTION_REQUESTED`
- **Role:** `PROCESS_REQUEST`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-005`
- **Operations:** `PAY-OP-005`
- **Transitions:** `PAY-TR-007`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `financialInstitutionCode` | `FinancialInstitutionCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `allocationIntentFingerprint` | `EvidenceFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `requestedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Resolve the protected CUT/Treasury configuration for the Payment bank and allocation intent. Side effect: `IDEMPOTENT_PROTECTED_CONFIGURATION_RESOLUTION`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Inbound Treasury account.
- Treasury account token.
- Clear CUT account.

**Replay:** `RESOLUTION_REUSES_EVENT_ID_AND_CONFIGURATION_SCOPE`.

### `PAY-EVT-012` — `PaymentTreasuryAccountResolutionRecorded`

Payment has accepted the protected Treasury-account resolution outcome.

- **Fact kind:** `TREASURY_ACCOUNT_RESOLUTION_RECORDED`
- **Role:** `EVIDENCE_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-006`
- **Operations:** `PAY-OP-006`
- **Transitions:** `PAY-TR-011`, `PAY-TR-012`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `outcome` | `RESOLVED|REJECTED` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `treasuryConfigurationId` | `string` | `no` | `RESTRICTED_BANK_CONFIGURATION` |
| `configurationVersion` | `string` | `no` | `RESTRICTED_BANK_CONFIGURATION` |
| `maskedTreasuryAccountReference` | `string` | `no` | `RESTRICTED_DISPLAY` |
| `rejectionCode` | `FailureCode` | `no` | `INTERNAL_NON_SENSITIVE` |
| `evidenceFingerprint` | `EvidenceFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `acceptedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Treasury account token.
- Clear CUT account.
- Raw protected configuration.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-013` — `PaymentApprovedForPosting`

All authorization, banking, funds and Treasury evidence permits one posting instruction.

- **Fact kind:** `PAYMENT_APPROVED_FOR_POSTING`
- **Role:** `LIFECYCLE_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-006`
- **Operations:** `PAY-OP-006`
- **Transitions:** `PAY-TR-011`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `financialInstitutionCode` | `FinancialInstitutionCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `requestedAmount` | `MoneyPayload` | `yes` | `RESTRICTED_FINANCIAL` |
| `approvedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-014` — `PaymentPostingAuthorized`

The sole logical posting instruction has been durably authorized.

- **Fact kind:** `POSTING_AUTHORIZED`
- **Role:** `FINANCIAL_AUTHORIZATION_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-007`
- **Operations:** `PAY-OP-007`
- **Transitions:** `PAY-TR-009`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `postingInstructionId` | `PostingInstructionId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `postingIdempotencyKey` | `PostingIdempotencyKey` | `yes` | `RESTRICTED_OPERATIONAL_REFERENCE` |
| `postingInstructionFingerprint` | `PostingInstructionFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `authorizedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Debtor account token.
- Treasury account token.
- Clear account.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-015` — `PaymentPostingRequested`

The sole authorized posting has been durably requested.

- **Fact kind:** `POSTING_REQUESTED`
- **Role:** `PROCESS_REQUEST`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-007`
- **Operations:** `PAY-OP-007`
- **Transitions:** `PAY-TR-009`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `postingInstructionId` | `PostingInstructionId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `postingIdempotencyKey` | `PostingIdempotencyKey` | `yes` | `RESTRICTED_OPERATIONAL_REFERENCE` |
| `postingInstructionFingerprint` | `PostingInstructionFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `financialInstitutionCode` | `FinancialInstitutionCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `requestedAmount` | `MoneyPayload` | `yes` | `RESTRICTED_FINANCIAL` |
| `requestedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Create or reuse the unique posting workflow and obtain protected execution material through the approved internal port. Side effect: `AMPLITUDE_POSTING`; idempotency: `postingInstructionId+postingIdempotencyKey+instructionFingerprint`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Clear debtor account.
- Debtor account token in general integration event payload.
- Treasury account token in general integration event payload.
- A replacement posting identity.

**Replay:** `ACCOUNTING_REUSES_ORIGINAL_POSTING_IDENTITY_NEVER_BLIND_WRITE`.

### `PAY-EVT-016` — `PaymentPostingOutcomeRecorded`

Payment has accepted the current direct canonical posting outcome.

- **Fact kind:** `POSTING_OUTCOME_RECORDED`
- **Role:** `FINANCIAL_EVIDENCE_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-008`
- **Operations:** `PAY-OP-008`
- **Transitions:** `PAY-TR-013`, `PAY-TR-014`, `PAY-TR-015`, `PAY-TR-016`, `PAY-TR-017`, `PAY-TR-018`, `PAY-TR-019`, `PAY-TR-020`, `PAY-TR-021`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `postingInstructionId` | `PostingInstructionId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `outcome` | `COMPLETED|REJECTED_NO_FINANCIAL_EFFECT|DEBIT_CONFIRMED_CUT_CREDIT_PENDING|REVERSAL_REQUIRED|UNKNOWN` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `principalPostingReference` | `BankPostingReference.principal` | `no` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `debitLeg` | `PostingLegPayload` | `yes` | `RESTRICTED_FINANCIAL` |
| `cutCreditLeg` | `PostingLegPayload` | `yes` | `RESTRICTED_FINANCIAL` |
| `businessDate` | `LocalDate` | `no` | `INTERNAL_NON_SENSITIVE` |
| `rejectionCode` | `FailureCode` | `no` | `INTERNAL_NON_SENSITIVE` |
| `nextAction` | `PostingNextAction` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `evidenceFingerprint` | `EvidenceFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `acceptedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Correlate Payment decision with the canonical posting/reconciliation workflow. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Post-debit balance.
- Clear account.
- Raw core-banking entries.
- Raw Amplitude response.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-017` — `PaymentEndOfDayTrackingRequested`

Payment has durably requested TFJ finality tracking for a completed posting.

- **Fact kind:** `TFJ_TRACKING_REQUESTED`
- **Role:** `PROCESS_REQUEST`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-008`, `PAY-CMD-009`
- **Operations:** `PAY-OP-008`, `PAY-OP-009`
- **Transitions:** `PAY-TR-013`, `PAY-TR-019`, `PAY-TR-022`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `financialInstitutionCode` | `FinancialInstitutionCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `principalPostingReference` | `BankPostingReference.principal` | `yes` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `businessDate` | `LocalDate` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `requestedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Register unique TFJ matching and scheduled read-only fallback reconciliation. Side effect: `TFJ_TRACKING_AND_LOOKUP`; idempotency: `paymentId+principalPostingReference+businessDate`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `REUSE_EXISTING_TFJ_TRACKER_READ_LOOKUP_ONLY`.

### `PAY-EVT-018` — `PaymentDebitConfirmed`

The debtor debit is confirmed while complete CUT credit is not yet confirmed.

- **Fact kind:** `DEBIT_CONFIRMED`
- **Role:** `FINANCIAL_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-008`, `PAY-CMD-009`
- **Operations:** `PAY-OP-008`, `PAY-OP-009`
- **Transitions:** `PAY-TR-014`, `PAY-TR-023`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `postingInstructionId` | `PostingInstructionId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `principalPostingReference` | `BankPostingReference.principal` | `yes` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `debitLegReference` | `BankPostingReference.debitLeg` | `no` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `businessDate` | `LocalDate` | `no` | `INTERNAL_NON_SENSITIVE` |
| `debitedAt` | `Instant` | `no` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Update reconciliation views without issuing another debit. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `FINANCIAL_REPLAY_FORBIDDEN_FACT_ONLY`.

### `PAY-EVT-019` — `PaymentPostingOutcomeLookupRequested`

Payment has durably requested authoritative resolution of an uncertain posting.

- **Fact kind:** `POSTING_OUTCOME_LOOKUP_REQUESTED`
- **Role:** `PROCESS_REQUEST`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-008`
- **Operations:** `PAY-OP-008`
- **Transitions:** `PAY-TR-015`, `PAY-TR-020`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `postingInstructionId` | `PostingInstructionId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `postingIdempotencyKey` | `PostingIdempotencyKey` | `yes` | `RESTRICTED_OPERATIONAL_REFERENCE` |
| `principalPostingReference` | `BankPostingReference.principal` | `no` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `lookupMode` | `IDEMPOTENCY_KEY|BANK_REFERENCE` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `unknownSince` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `requestedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Perform authoritative read-only posting lookup using the original instruction identity. Side effect: `AMPLITUDE_READ_ONLY_LOOKUP`; idempotency: `postingInstructionId+lookupMode+eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `READ_ONLY_LOOKUP_ALLOWED_BLIND_POSTING_REPLAY_FORBIDDEN`.

### `PAY-EVT-020` — `PaymentReversalRequired`

Payment requires explicit reversal review because a financial effect exists or is authoritatively reconciled.

- **Fact kind:** `REVERSAL_REVIEW_REQUESTED`
- **Role:** `LIFECYCLE_AND_PROCESS_REQUEST`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-008`, `PAY-CMD-009`, `PAY-CMD-010`, `PAY-CMD-012`, `PAY-CMD-013`
- **Operations:** `PAY-OP-008`, `PAY-OP-009`, `PAY-OP-010`, `PAY-OP-012`, `PAY-OP-013`
- **Transitions:** `PAY-TR-018`, `PAY-TR-021`, `PAY-TR-026`, `PAY-TR-028`, `PAY-TR-033`, `PAY-TR-035`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `principalPostingReference` | `BankPostingReference.principal` | `yes` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `reasonCode` | `FailureCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `sourceStage` | `POSTING|TFJ|REVERSAL` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `knownDebitStatus` | `PostingLegStatus` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `knownCutCreditStatus` | `PostingLegStatus` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `requiredAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Open or reuse the reversal/reconciliation case; do not reverse automatically. Side effect: `REVERSAL_CASE_ONLY`; idempotency: `paymentId+principalPostingReference+reasonCode`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `OPEN_OR_REUSE_CASE_AUTOMATIC_REVERSAL_FORBIDDEN`.

### `PAY-EVT-021` — `PaymentPostingOutcomeResolved`

An authoritative lookup has resolved the original uncertain posting.

- **Fact kind:** `POSTING_OUTCOME_RESOLVED`
- **Role:** `FINANCIAL_EVIDENCE_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-009`
- **Operations:** `PAY-OP-009`
- **Transitions:** `PAY-TR-022`, `PAY-TR-023`, `PAY-TR-024`, `PAY-TR-025`, `PAY-TR-026`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `postingInstructionId` | `PostingInstructionId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `previousOutcome` | `UNKNOWN` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `resolvedOutcome` | `COMPLETED|REJECTED_NO_FINANCIAL_EFFECT|DEBIT_CONFIRMED_CUT_CREDIT_PENDING|REVERSAL_REQUIRED` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `principalPostingReference` | `BankPostingReference.principal` | `no` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `debitLeg` | `PostingLegPayload` | `yes` | `RESTRICTED_FINANCIAL` |
| `cutCreditLeg` | `PostingLegPayload` | `yes` | `RESTRICTED_FINANCIAL` |
| `businessDate` | `LocalDate` | `no` | `INTERNAL_NON_SENSITIVE` |
| `rejectionCode` | `FailureCode` | `no` | `INTERNAL_NON_SENSITIVE` |
| `evidenceFingerprint` | `EvidenceFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `resolvedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Close or update the posting reconciliation case. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `FINANCIAL_REPLAY_FORBIDDEN_FACT_ONLY`.

### `PAY-EVT-022` — `PaymentEndOfDayConfirmationRecorded`

Payment has accepted a uniquely matched final TFJ confirmation.

- **Fact kind:** `TFJ_CONFIRMATION_RECORDED`
- **Role:** `TREASURY_EVIDENCE_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-010`
- **Operations:** `PAY-OP-010`
- **Transitions:** `PAY-TR-027`, `PAY-TR-028`, `PAY-TR-029`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `confirmationId` | `TfjConfirmationId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `financialInstitutionCode` | `FinancialInstitutionCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `businessDate` | `LocalDate` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `principalPostingReference` | `BankPostingReference.principal` | `yes` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `tfjStatus` | `INTEGRATED|FAILED` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `failureCode` | `FailureCode` | `no` | `INTERNAL_NON_SENSITIVE` |
| `recoveryAction` | `MANUAL_RECONCILIATION|REVERSAL_REVIEW|REVERSAL_REQUIRED` | `no` | `INTERNAL_NON_SENSITIVE` |
| `confirmedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `matchedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `evidenceFingerprint` | `EvidenceFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |

**Consumers**

- `accounting` — Close or update the uniquely matched TFJ reconciliation workflow. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Raw TFJ payload or file.
- Unmatched confirmation data.
- Clear CUT or debtor account.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-023` — `TreasuryIntegrationConfirmed`

Uniquely matched TFJ INTEGRATED evidence has established Treasury finality.

- **Fact kind:** `TREASURY_INTEGRATION_CONFIRMED`
- **Role:** `TERMINAL_FINANCIAL_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-010`
- **Operations:** `PAY-OP-010`
- **Transitions:** `PAY-TR-027`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `confirmationId` | `TfjConfirmationId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `principalPostingReference` | `BankPostingReference.principal` | `yes` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `businessDate` | `LocalDate` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `confirmedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `customer` — Update ObservedCustomer success and finality counters. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `FINANCIAL_REPLAY_FORBIDDEN_FACT_ONLY`.

### `PAY-EVT-024` — `PaymentFinalResultAvailable`

The final Treasury-integrated result is durably available for delivery.

- **Fact kind:** `FINAL_RESULT_AVAILABLE`
- **Role:** `RESULT_INTENT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-010`
- **Operations:** `PAY-OP-010`
- **Transitions:** `PAY-TR-027`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `externalPaymentReference` | `ExternalPaymentReference` | `yes` | `BUSINESS_REFERENCE` |
| `resultType` | `TREASURY_INTEGRATED` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `principalPostingReference` | `BankPostingReference.principal` | `yes` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `businessDate` | `LocalDate` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `confirmationId` | `TfjConfirmationId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `availableAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `notification` — Create or reuse the final TRESOR PAY NotificationDelivery. Side effect: `TRESOR_PAY_NOTIFICATION`; idempotency: `sourceEventId+FINAL`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `NOTIFICATION_REUSES_SOURCE_EVENT_ID_AND_PHASE`.

### `PAY-EVT-025` — `PaymentTreasuryReconciliationRequired`

A matched adverse TFJ result requires manual reconciliation or reversal review.

- **Fact kind:** `TREASURY_RECONCILIATION_REQUIRED`
- **Role:** `PROCESS_REQUEST`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-010`
- **Operations:** `PAY-OP-010`
- **Transitions:** `PAY-TR-029`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `confirmationId` | `TfjConfirmationId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `principalPostingReference` | `BankPostingReference.principal` | `yes` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `businessDate` | `LocalDate` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `failureCode` | `FailureCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `recoveryAction` | `MANUAL_RECONCILIATION|REVERSAL_REVIEW` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `requiredAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Open or reuse a manual Treasury reconciliation case. Side effect: `RECONCILIATION_CASE_ONLY`; idempotency: `confirmationId+paymentId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `OPEN_OR_REUSE_RECONCILIATION_CASE_NO_FINANCIAL_WRITE`.

### `PAY-EVT-026` — `PaymentReversalAuthorized`

One reversal instruction has been explicitly and durably authorized.

- **Fact kind:** `REVERSAL_AUTHORIZED`
- **Role:** `FINANCIAL_AUTHORIZATION_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-011`
- **Operations:** `PAY-OP-011`
- **Transitions:** `PAY-TR-030`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `reversalInstructionId` | `ReversalInstructionId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `reversalIdempotencyKey` | `ReversalIdempotencyKey` | `yes` | `RESTRICTED_OPERATIONAL_REFERENCE` |
| `originalPostingReference` | `BankPostingReference.principal` | `yes` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `authorizationType` | `BANK_INSTRUCTION|APPROVED_RUNBOOK` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `authorizationReference` | `ReversalAuthorizationReference` | `yes` | `RESTRICTED_OPERATIONAL_REFERENCE` |
| `reasonCode` | `FailureCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `authorizedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Bank instruction document.
- Operator credential.
- Free-form operator comment.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-027` — `PaymentReversalRequested`

The explicitly authorized reversal has been durably requested.

- **Fact kind:** `REVERSAL_REQUESTED`
- **Role:** `PROCESS_REQUEST`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-011`
- **Operations:** `PAY-OP-011`
- **Transitions:** `PAY-TR-030`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `reversalInstructionId` | `ReversalInstructionId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `reversalIdempotencyKey` | `ReversalIdempotencyKey` | `yes` | `RESTRICTED_OPERATIONAL_REFERENCE` |
| `originalPostingReference` | `BankPostingReference.principal` | `yes` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `instructionFingerprint` | `EvidenceFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `requestedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Submit or reuse the explicitly authorized reversal instruction. Side effect: `AMPLITUDE_REVERSAL`; idempotency: `reversalInstructionId+reversalIdempotencyKey+instructionFingerprint`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Original posting idempotency key reuse.
- Clear account.
- Raw bank instruction.

**Replay:** `ACCOUNTING_REUSES_ORIGINAL_REVERSAL_IDENTITY_NEVER_BLIND_WRITE`.

### `PAY-EVT-028` — `PaymentReversalOutcomeRecorded`

Payment has accepted the current direct canonical reversal outcome.

- **Fact kind:** `REVERSAL_OUTCOME_RECORDED`
- **Role:** `FINANCIAL_EVIDENCE_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-012`
- **Operations:** `PAY-OP-012`
- **Transitions:** `PAY-TR-031`, `PAY-TR-032`, `PAY-TR-033`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `reversalInstructionId` | `ReversalInstructionId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `outcome` | `REVERSED|REJECTED|NOT_ALLOWED|UNKNOWN` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `reversalReference` | `ReversalReference` | `no` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `reversalEntryReference` | `string` | `no` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `reasonCode` | `FailureCode` | `no` | `INTERNAL_NON_SENSITIVE` |
| `evidenceFingerprint` | `EvidenceFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `acceptedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Update the reversal and reconciliation workflow. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-029` — `PaymentReversalResultAvailable`

A conclusive reversal result is durably available for delivery.

- **Fact kind:** `REVERSAL_RESULT_AVAILABLE`
- **Role:** `RESULT_INTENT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-012`, `PAY-CMD-013`
- **Operations:** `PAY-OP-012`, `PAY-OP-013`
- **Transitions:** `PAY-TR-031`, `PAY-TR-033`, `PAY-TR-034`, `PAY-TR-035`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `externalPaymentReference` | `ExternalPaymentReference` | `yes` | `BUSINESS_REFERENCE` |
| `resultType` | `REVERSED|REVERSAL_REQUIRED` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `originalPostingReference` | `BankPostingReference.principal` | `yes` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `reversalReference` | `ReversalReference` | `no` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `failureCode` | `FailureCode` | `no` | `INTERNAL_NON_SENSITIVE` |
| `availableAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `notification` — Create or reuse the reversal-phase TRESOR PAY NotificationDelivery. Side effect: `TRESOR_PAY_NOTIFICATION`; idempotency: `sourceEventId+REVERSAL`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `NOTIFICATION_REUSES_SOURCE_EVENT_ID_AND_PHASE`.

### `PAY-EVT-030` — `PaymentReversalOutcomeLookupRequested`

Payment has durably requested resolution of an uncertain reversal outcome.

- **Fact kind:** `REVERSAL_OUTCOME_LOOKUP_REQUESTED`
- **Role:** `PROCESS_REQUEST`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-012`
- **Operations:** `PAY-OP-012`
- **Transitions:** `PAY-TR-032`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `reversalInstructionId` | `ReversalInstructionId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `reversalIdempotencyKey` | `ReversalIdempotencyKey` | `yes` | `RESTRICTED_OPERATIONAL_REFERENCE` |
| `reversalReference` | `ReversalReference` | `no` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `requestedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Perform authoritative read-only lookup for the original reversal instruction. Side effect: `AMPLITUDE_READ_ONLY_LOOKUP`; idempotency: `reversalInstructionId+eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `READ_ONLY_LOOKUP_ALLOWED_BLIND_REVERSAL_REPLAY_FORBIDDEN`.

### `PAY-EVT-031` — `PaymentReversalOutcomeResolved`

An authoritative lookup has resolved the original uncertain reversal.

- **Fact kind:** `REVERSAL_OUTCOME_RESOLVED`
- **Role:** `FINANCIAL_EVIDENCE_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-013`
- **Operations:** `PAY-OP-013`
- **Transitions:** `PAY-TR-034`, `PAY-TR-035`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `reversalInstructionId` | `ReversalInstructionId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `previousOutcome` | `UNKNOWN` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `resolvedOutcome` | `REVERSED|REJECTED|NOT_ALLOWED` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `reversalReference` | `ReversalReference` | `no` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `reversalEntryReference` | `string` | `no` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `reasonCode` | `FailureCode` | `no` | `INTERNAL_NON_SENSITIVE` |
| `evidenceFingerprint` | `EvidenceFingerprint` | `yes` | `INTERNAL_SECURITY_METADATA` |
| `resolvedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `accounting` — Close or update the reversal outcome-reconciliation case. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-032` — `PaymentFailedWithoutFinancialEffect`

Payment has reached terminal FAILED with proven absence of financial effect.

- **Fact kind:** `PAYMENT_FAILED_WITHOUT_FINANCIAL_EFFECT`
- **Role:** `TERMINAL_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-008`, `PAY-CMD-009`, `PAY-CMD-016`
- **Operations:** `PAY-OP-008`, `PAY-OP-009`, `PAY-OP-016`
- **Transitions:** `PAY-TR-017`, `PAY-TR-025`, `PAY-TR-038`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `failureCode` | `FailureCode` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `failureCategory` | `FailureCategory` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `failureStage` | `FailureStage` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `finalizedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `customer` — Update ObservedCustomer technical-failure counters and last outcome. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Additionally forbidden**

- Stack trace.
- Endpoint or topology.
- Raw provider error.
- Customer/account data.

**Replay:** `FORBIDDEN_PROJECTION_REPLAY_ONLY`.

### `PAY-EVT-033` — `PaymentReversed`

The explicitly authorized reversal is confirmed while original posting evidence remains.

- **Fact kind:** `PAYMENT_REVERSED`
- **Role:** `TERMINAL_FINANCIAL_FACT`
- **Schema version:** `1`
- **Commands:** `PAY-CMD-012`, `PAY-CMD-013`
- **Operations:** `PAY-OP-012`, `PAY-OP-013`
- **Transitions:** `PAY-TR-031`, `PAY-TR-034`

**Event-specific payload**

| Field | Type | Required | Classification |
| --- | --- | --- | --- |
| `reversalInstructionId` | `ReversalInstructionId` | `yes` | `INTERNAL_NON_SENSITIVE` |
| `originalPostingReference` | `BankPostingReference.principal` | `yes` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `reversalReference` | `ReversalReference` | `yes` | `RESTRICTED_FINANCIAL_REFERENCE` |
| `reversedAt` | `Instant` | `yes` | `INTERNAL_NON_SENSITIVE` |

**Consumers**

- `customer` — Update ObservedCustomer final reversal outcome. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.
- `reporting` — Update Payment search and immutable timeline projections. Side effect: `PROJECTION_ONLY`; idempotency: `eventId`.

**Replay:** `FINANCIAL_REPLAY_FORBIDDEN_FACT_ONLY`.


## Policy and service bindings added by Lot 2.7

Every event payload passes `PAY-POL-014 PaymentEventDisclosurePolicy` before
Outbox persistence.

Additional bindings include:

| Event family | Decision components |
| --- | --- |
| Authorization/banking/funds/Treasury evidence | Corresponding acceptance policy + replay policy |
| Posting authorization/request | `PAY-POL-007` |
| Posting outcome facts | `PAY-DS-001` |
| TFJ facts | `PAY-DS-002` |
| Reversal facts | `PAY-DS-003` |
| Result-intent events | `PAY-POL-013`, `PAY-DS-004` |

Disclosure validation does not decrypt, retrieve or infer protected material.
It validates only the explicit payload already constructed from approved
fields.

## 7. Notification ownership

Only these Payment events trigger Notification:

```text
PaymentImmediateResultAvailable
PaymentFinalResultAvailable
PaymentReversalResultAvailable
```

Notification creates its own `NotificationDelivery` state and process events.

A notification delivery success, retry, failure or DLQ:

- does not return as a Payment command;
- does not change Payment status;
- is visible through reporting/audit projections.

## 8. Process request security

General integration-event payloads never contain debtor or Treasury account
tokens.

A consumer needing protected execution material uses an approved secure
internal port keyed by:

```text
PaymentId
aggregateVersion
instruction identifier when applicable
```

Posting and reversal events still carry their stable instruction and
idempotency identities so that the supporting process cannot invent another
financial command.

The exact secure instruction-access policy is finalized in Lot 2.7.

## 9. Versioning

- event type is the exact stable domain-record name;
- initial `schemaVersion` is `1`;
- optional additive fields require consumer review;
- required field addition, rename, removal or semantic change requires a new
  schema version;
- the version is not appended to the event type;
- a retired event type is never reused.

## 10. IA-0P migration

Important replacements:

| IA-0P event | IA-1 event(s) |
| --- | --- |
| `PaymentRequestReceived` | `PaymentReceived` |
| `PaymentRequestRejected` | `PaymentRejected`, `PaymentImmediateResultAvailable` |
| `BankingVerificationCompleted` | `PaymentBankingVerificationRecorded`, `PaymentFundsControlRecorded` |
| `PaymentPostingStarted` | `PaymentPostingAuthorized`, `PaymentPostingRequested` |
| `TreasuryAccountCredited` | Posting outcome, result availability and TFJ tracking events |
| `PaymentPostingOutcomeUnknown` | Posting outcome plus lookup-request and result events |
| `PaymentPendingEndOfDayConfirmation` | `PaymentEndOfDayTrackingRequested` |
| `PaymentNotificationDelivered` | removed; owned by Notification |
| `PaymentFailed` | `PaymentFailedWithoutFinancialEffect` |
| `PaymentReversed` | explicit terminal fact plus reversal-result intent |

## 11. Decisions closed in Lot 2.6

| Decision | Result |
| --- | --- |
| `PAY-DEC-IA1-041` | Catalogue contains only events registered by Payment. |
| `PAY-DEC-IA1-042` | Payment domain contract maps explicitly to the existing integration envelope. |
| `PAY-DEC-IA1-043` | 33 stable event types map one-to-one to canonical fact kinds. |
| `PAY-DEC-IA1-044` | Multiple events share aggregate version and use event sequence. |
| `PAY-DEC-IA1-045` | Payloads are explicit minimized safe views. |
| `PAY-DEC-IA1-046` | Process-request replay uses business idempotency. |
| `PAY-DEC-IA1-047` | Only result-intent events trigger Notification. |
| `PAY-DEC-IA1-048` | Event names are stable and schemas versioned. |
| `PAY-DEC-IA1-049` | `PaymentReversed` is distinct from reversal notification intent. |
| `PAY-DEC-IA1-050` | Deferred/partial and conclusive reversal paths register their result intents. |

## 12. Exit checklist

- [ ] Every state-machine fact kind maps to exactly one event ID.
- [ ] Every event references valid commands, operations and transitions.
- [ ] Every event has an explicit safe payload.
- [ ] No protected object is serialized automatically.
- [ ] Multiple-event ordering is deterministic.
- [ ] Notification triggers are unique and explicit.
- [ ] Financial process-request events define business idempotency.
- [ ] IA-0P process events are removed from Payment ownership.
- [ ] Event and state-machine catalogues are cross-file consistent.
- [ ] Code generation remains forbidden.

## 13. Verdict

```text
IA-1 LOT 2.6 PAYMENT DOMAIN EVENTS PREPARED
EVENT COUNT: 33
STATUS: DRAFT_PENDING_VALIDATION
NEXT: OWNER APPROVAL AND CONTRACT GATE CLOSURE
CODE GENERATION: FORBIDDEN
```

## Final Lot 2.8 validation

The cross-catalogue validation passed.

```text
MODEL STATUS: FINAL_VALIDATED
LOT 2 STATUS: COMPLETE
MODEL BLOCKERS: NONE
GENERATION READINESS: READY_PENDING_EXTERNAL_APPROVALS
CODE GENERATION: FORBIDDEN_PENDING_EXPLICIT_APPROVAL
```

Normative validation evidence:

- `PAYMENT_MODEL_VALIDATION_REPORT.md`
- `PAYMENT_MODEL_VALIDATION.yaml`
- `PAYMENT_ACCEPTANCE_SCENARIOS.md`
