# SIXPAY CONNECT — Accounting Module

## Purpose

The Accounting module owns SIXPAY accounting-batch constitution, submission
tracking and reconciliation behavior.

The `partner` module remains the golden business-module reference for structure
and testing discipline.

---

## Current responsibilities

The current Accounting implementation includes:

```text
Domain
  accounting batches
  accounting batch items
  tracking
  submission state
  eligibility policies
  cutoff policies

Application
  batch constitution
  batch building
  idempotency-key generation
  batch selection
  reconciliation

Infrastructure
  PostgreSQL persistence
  accounting-provider REST integration
  OAuth2 access-token acquisition
  provider DTO mapping and validation
```

No module-owned inbound HTTP controller is currently implemented.

Therefore:

```text
Accounting API coverage = N/A
```

for the current implementation.

---

## Domain model

Representative domain types include:

```text
AccountingBatch
AccountingBatchItem
AccountingBatchTracking
AccountingBatchItemTracking
AccountingPaymentCandidate
AccountingProviderBatchResult
AccountingProviderItemResult
AccountingSubmissionState
```

Representative policies include:

```text
DailyAccountingCutoffPolicy
VerifiedTresorPayStatusEligibilityPolicy
```

The domain remains free of JPA concerns.

---

## Application behavior

The application layer owns, among other responsibilities:

```text
AccountingBatchConstitutionService
AccountingBatchBuilder
AccountingBatchIdempotencyKeyFactory
AccountingBatchSelectionService
AccountingBatchReconciliationService
```

Financial reconciliation must preserve the repository-wide rule:

```text
unknown financial outcome
    -> reconcile first
    -> never blindly resubmit
```

---

## Persistence

Accounting persistence is owned by:

```text
AccountingBatchRepositoryAdapter
AccountingBatchTrackingRepositoryAdapter
```

with Spring Data repositories and JPA entities kept inside:

```text
com.sixpay.accounting.infrastructure.persistence
```

Database-level guards include:

```text
unique accounting-batch idempotency key
unique Payment assignment to an accounting batch
```

PostgreSQL behavioral tests use Testcontainers and the module Flyway migrations.

---

## Provider integration

Accounting-provider-specific behavior remains owned by Accounting.

Representative infrastructure includes:

```text
OAuth2AccountingApiAccessTokenProvider
RestAccountingBatchClient
AccountingApiMapper
AccountingApiResponseValidator
AccountingApiProperties
```

Provider-neutral HTTP/resilience capabilities remain in `backend/integration`.

---

## Test strategy

The module follows the same layered strategy as the golden `partner` module.

```text
Domain
    -> pure tests

Application
    -> focused service tests

API
    -> N/A until an Accounting inbound API exists

Infrastructure
    -> focused adapter tests and PostgreSQL integration tests
```

Do not build one large Spring test for all Accounting responsibilities.

---

## Existing focused coverage

Current focused tests include:

```text
AccountingBatchConstitutionServiceTest
AccountingBatchIdempotencyKeyFactoryTest
AccountingBatchReconciliationServiceTest

DailyAccountingCutoffPolicyTest
VerifiedTresorPayStatusEligibilityPolicyTest

AccountingApiMapperTest
AccountingApiPropertiesTest

AccountingArchitectureTest
AccountingApiArchitectureTest
AccountingClockWiringArchitectureTest
AccountingPersistenceArchitectureTest
AccountingReconciliationArchitectureTest
```

Phase 8.2.4 adds a real PostgreSQL persistence test so architecture/source
inspection is complemented by runtime persistence evidence.

---

## Validation

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress     -pl accounting -am test
```

Then:

```bash
mvn --batch-mode --no-transfer-progress     -pl accounting -am clean verify
```

Full integration validation:

```bash
mvn --batch-mode --no-transfer-progress     -pl accounting -am -Pfull-tests clean verify
```

Docker is required for Testcontainers-backed integration tests.

---

## Phase 8 status

After Phase 8.2.4:

```text
Domain          COVERED
Application     COVERED
API             N/A
Infrastructure  COVERED
```

Detailed evidence is maintained in:

```text
ACCOUNTING-TEST-COVERAGE.md
```

---

## Final rule

Accounting is responsible for accounting-domain behavior and
provider-specific accounting mappings.

Cross-module pilot workflows belong to Phase 8.3.

Consistency with the `partner` golden module takes precedence over convenience.
