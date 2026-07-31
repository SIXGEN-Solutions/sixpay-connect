# SIXPAY CONNECT — Payment Domain Generation Brief

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `3.2 — Identifiers, Value Objects and classifications`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `LOT_3_2_IMPLEMENTED`  
> **Global code generation:** **FORBIDDEN**  
> **Current increment:** **AUTHORIZED**

## Governing model

The Lot 2.8 IA-1 model remains frozen. Lot 3.2 translates only its base
identifier, Value Object and classification catalogue into Java 21.

## Authorization

```text
PAY-AUTH-IA1-001
scope: PAYMENT_DOMAIN_ONLY
currentIncrement: LOT_3_2_IDENTIFIERS_VALUE_OBJECTS
currentIncrementCodeGenerationAllowed: true
futureIncrementActivationRequired: true
globalCodeGenerationAllowed: false
```

## Implemented types

```text
22 Payment-local production types
+ package-private structural validation utility
+ reused Money, CorrelationId and ValueObject
```

Implemented:

```text
PaymentId
PaymentSource
ExternalPaymentReference
ExternalSubscriptionReference
PublicPaymentReference
IdempotencyKey
RequestFingerprint
PaymentRequestIdentity
FinancialInstitutionCode
DebtorAccountReference
TreasuryAccountReference
TreasuryBeneficiaryReference
TreasuryAllocation
TreasuryAllocationIntent
BankPostingReference
FailureCode
FailureCategory
FailureStage
RetryDisposition
ExternalSystem
PaymentFailure
PaymentStatus
```

## Deferred types

The following are not authorized by Lot 3.2:

```text
snapshot-support identifiers and EvidenceMetadata
PostingInstructionIdentity and ReversalInstructionIdentity
PaymentCommandId and ExpectedBusinessVersion
event metadata Value Objects
PaymentState and Payment Aggregate Root
policies and Domain Services
domain events
```

## Structural rules implemented

- canonical non-nil Payment UUID;
- exact case-sensitive external references;
- `PAY-` Crockford ULID public reference;
- strict idempotency key and SHA-256 fingerprint;
- Payment-specific UUID validation around reused `CorrelationId`;
- normalized financial institution code;
- protected account token/mask/fingerprint separation;
- canonical immutable Treasury allocations;
- exact allocation total and currency;
- opaque bank posting references;
- stable failure code and category/disposition matrix;
- 17 final IA-1 statuses with four terminal values.

## Scope protections

Global generation blockers remain active.

No application layer, API, database, Outbox, external adapter, Spring
configuration, snapshot, Aggregate Root, policy or event is generated.

## Tests

Five unit-test classes and the architecture test provide the Lot 3.2 evidence.

## Verdict

```text
LOT 3.2: IMPLEMENTED
LOCAL TYPES: 22
MODEL SEMANTICS: UNCHANGED
GLOBAL GENERATION: FORBIDDEN
NEXT: LOT 3.3 EXPLICIT ACTIVATION
```
