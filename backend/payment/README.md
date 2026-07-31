# SIXPAY CONNECT — Payment Module

## Status

```text
Current increment: Lot 3.2 — Identifiers, Value Objects and classifications
Implementation scope: PAYMENT_DOMAIN_ONLY
Global generation: FORBIDDEN
Current increment generation: AUTHORIZED
```

## Implemented domain concepts

Lot 3.2 implements 22 local types:

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

The module reuses, rather than duplicates:

```text
com.sixpay.common.context.CorrelationId
com.sixpay.sharedkernel.domain.valueobject.Money
com.sixpay.sharedkernel.domain.valueobject.ValueObject
```

## Package

```text
src/main/java/com/sixpay/payment/domain/model/
```

All types are immutable. String-based types normalize only according to their
validated IA-1 catalogue rules.

Protected account references are ordinary final classes, not records, so their
default representation cannot expose protected tokens.

## Deliberately deferred

Lot 3.2 does not implement:

- `Payment` or `PaymentState`;
- snapshot/evidence-support Value Objects;
- posting/reversal instruction identities;
- command and event metadata;
- policies or Domain Services;
- domain events;
- application, API, persistence or adapters.

These belong to subsequent explicitly activated increments.

## Tests

```text
PaymentIdentityValueObjectsTest
ProtectedAccountValueObjectsTest
TreasuryAllocationIntentTest
PaymentFailureTest
PaymentClassificationTest
PaymentArchitectureTest
```

The tests cover formats, normalization, equality, defensive copying,
confidentiality, failure matrices, the 17 statuses and architecture boundaries.

## Build

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress -pl payment -am test
```

## Next increment

```text
Lot 3.3 — Snapshots and financial evidence support
```

It requires explicit activation before implementation.
