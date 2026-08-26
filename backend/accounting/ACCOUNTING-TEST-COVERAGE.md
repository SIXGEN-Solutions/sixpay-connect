# SIXPAY CONNECT — Accounting Golden Test Coverage

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.4 — Accounting
```

## 1. Golden reference

The `partner` module remains the golden business-module reference.

Accounting is evaluated independently across:

```text
Domain
Application
API
Infrastructure
```

---

## 2. Domain coverage

Accounting already contains focused domain-policy tests:

```text
DailyAccountingCutoffPolicyTest
VerifiedTresorPayStatusEligibilityPolicyTest
```

Application tests also exercise real Accounting domain objects and their
invariants rather than mocking the domain.

Architecture tests confirm that JPA remains outside domain/application.

Status:

```text
DOMAIN = COVERED
```

---

## 3. Application coverage

Direct behavioral evidence includes:

```text
AccountingBatchConstitutionServiceTest
AccountingBatchIdempotencyKeyFactoryTest
AccountingBatchReconciliationServiceTest
```

Important covered behaviors include:

```text
candidate selection
already-assigned Payment exclusion
batch constitution
idempotency-key generation
submission/reconciliation behavior
unknown submission outcome protection
```

In particular, reconciliation tests enforce the critical financial rule:

```text
unknown submission outcome
    -> do not blindly resubmit
    -> resolve by reconciliation
```

Status:

```text
APPLICATION = COVERED
```

---

## 4. API coverage

The current Accounting module does not expose a module-owned inbound HTTP
controller.

Its `api` package is not an inbound REST API equivalent to Payment Query or
Partner API.

Accounting-provider HTTP integration is outbound infrastructure.

Status:

```text
API = N/A
```

No artificial Accounting controller test is introduced.

---

## 5. Infrastructure coverage before 8.2.4

Focused provider tests already include:

```text
AccountingApiMapperTest
AccountingApiPropertiesTest
```

Architecture evidence includes:

```text
AccountingPersistenceArchitectureTest
AccountingApiArchitectureTest
```

`AccountingPersistenceArchitectureTest` verifies source-level database guards
and confirms JPA remains in infrastructure.

However source inspection alone does not prove runtime PostgreSQL behavior.

The implementation contains:

```text
AccountingBatchRepositoryAdapter
AccountingBatchTrackingRepositoryAdapter
AccountingBatchSpringDataRepository
AccountingBatchTrackingSpringDataRepository
AccountingBatchJpaEntity
AccountingBatchItemJpaEntity
AccountingBatchTrackingJpaEntity
AccountingBatchItemTrackingJpaEntity
```

and the module already declares:

```text
Flyway
PostgreSQL
Testcontainers PostgreSQL
```

so runtime persistence testing is an intended repository pattern.

---

## 6. Phase 8.2.4 persistence addition

This lot adds:

```text
AccountingPersistenceIT
```

The test follows the golden `PartnerPersistenceIT` strategy:

```text
@SpringBootTest with explicit TestApplication
PostgreSQL Testcontainers
Dynamic datasource properties
real Spring Data repositories
real Accounting persistence adapters
Flyway migrations
```

It remains focused on Accounting persistence only.

Covered runtime behaviors:

```text
AccountingBatch save + findById round-trip
findByIdempotencyKey round-trip
assigned Payment lookup
AccountingBatchTracking save + findByBatchId round-trip
database uniqueness guard for idempotency key
database uniqueness guard for Payment assignment
domain conflict exception translation
```

Status after this addition:

```text
INFRASTRUCTURE = COVERED
```

---




## BigDecimal persistence comparison

The Accounting domain does not define `BigDecimal.scale()` as a business
invariant. PostgreSQL/JPA stores `amount` using a persistence scale of four
decimal places, while an in-memory domain value may have another equivalent
scale.

Therefore persistence round-trip assertions compare monetary values
numerically:

```java
assertThat(savedItem.amount())
        .isEqualByComparingTo(reloadedItem.amount());
```

The test deliberately does not require `BigDecimal.equals()` because that
method treats numerically equal values with different scales as unequal.

Example:

```text
12500.00    != 12500.0000    with BigDecimal.equals
12500.00    == 12500.0000    numerically
```

No production canonicalization is introduced solely to satisfy the test.

---

## Test fixture validity

`AccountingBatchIdempotencyKey` enforces the domain invariant:

```text
^[a-f0-9]{64}$
```

Persistence test fixtures therefore use valid lowercase 64-character SHA-256
hex strings. Human-readable labels such as `acct-20260809-001` are invalid
domain values and must not be used in integration tests.

The duplicate-key scenario intentionally reuses the same valid 64-character
hex value across two different batches.

---

## Test bootstrap alignment

`AccountingPersistenceIT` delegates JPA entity/repository scanning to
`AccountingModuleConfiguration`.

The test MUST NOT repeat:

```text
@EntityScan
@EnableJpaRepositories
manual persistence-adapter imports
```

because the Accounting auto-configuration already owns those registrations.
Duplicating them causes Spring Boot to register the same Spring Data
repositories twice.

The corrected test mirrors the `PartnerPersistenceIT` bootstrap pattern:

```text
@SpringBootConfiguration
@EnableAutoConfiguration
@ImportAutoConfiguration(AccountingModuleConfiguration.class)
```

---

## 7. Final 8.2.4 classification

| Dimension | Status |
|---|---|
| Domain | COVERED |
| Application | COVERED |
| API | N/A |
| Infrastructure | COVERED |

Overall:

```text
ACCOUNTING = COVERED
```

for module-local golden coverage.

Cross-module accounting workflows remain Phase 8.3 responsibilities.

---

## 8. Validation commands

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress     -pl accounting -am test
```

Integration test alone:

```bash
mvn --batch-mode --no-transfer-progress     -pl accounting     -DskipITs=false     -Dit.test=AccountingPersistenceIT     verify
```

Complete module integration validation:

```bash
mvn --batch-mode --no-transfer-progress     -pl accounting -am -Pfull-tests clean verify
```

---

## 9. Golden-module conformance

Do not introduce:

```text
AccountingTestEverything
AccountingFullFlowSpringTest
```

Use:

```text
domain policy
    -> pure test

application orchestration
    -> focused service test

database semantics
    -> PostgreSQL integration test

provider mapping
    -> focused adapter/mapper test

cross-module workflow
    -> Phase 8.3
```
