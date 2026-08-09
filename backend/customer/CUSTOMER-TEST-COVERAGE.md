# SIXPAY CONNECT — Customer Golden Test Coverage

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.1 — Customer
```

## 1. Reference

The `partner` module remains the golden business-module reference.

Customer is evaluated independently across:

```text
Domain
Application
API
Infrastructure
```

Tests are kept with the owning module.

Cross-module scenarios remain outside this lot and belong to Phase 8.3.

---

## 2. Customer bounded responsibilities

The Customer module currently contains two major capability families:

```text
customer
├── verification
│   ├── application
│   ├── domain
│   └── infrastructure/banking
│
└── observation
    ├── api
    ├── application
    ├── domain
    └── infrastructure
        ├── audit
        ├── persistence
        ├── query
        ├── resilience
        └── observability
```

The test strategy SHALL preserve those internal boundaries.

---

## 3. Domain coverage

### Observation domain

Direct behavioral evidence exists in:

```text
ObservedCustomerTest
```

It validates, among other things:

- valid reconstitution;
- defensive immutable collections;
- observation temporal ordering;
- payment-derived counters;
- total/payment-history consistency;
- duplicate payment identifiers;
- duplicate financial institutions;
- protection against sensitive-data leakage.

Status:

```text
COVERED
```

### Verification domain

The verification domain contains explicit value objects, policies and decision
services.

Its detailed completeness remains governed by existing verification-domain
tests. No blanket replacement test is introduced in 8.2.1.

Status:

```text
COVERED / PRESERVE EXISTING
```

---

## 4. Application coverage

### Observed Customer query

Direct behavioral coverage exists in:

```text
ObservedCustomerQueryServiceTest
```

Observed scenarios include:

- search cursor decoding;
- next-cursor creation;
- detail not found;
- temporary repository failure;
- payment cursor decoding;
- customer existence validation;
- payment next-cursor creation;
- unknown customer;
- oversized repository slice;
- invalid cursor;
- cursor infrastructure failure;
- explicit unavailable exception preservation.

This is aligned with the golden `partner` approach because the service is
tested without starting Spring.

Status:

```text
COVERED
```

### Architecture/conformance evidence

Additional evidence exists in:

```text
ObservedCustomerQueryLot477ApplicationTest
ObservedCustomerQueryContractConformanceTest
```

These tests are complementary architecture/contract checks.

They SHALL NOT replace behavioral service or HTTP tests.

---

## 5. API coverage

### Existing evidence before 8.2.1

Bootstrap contained:

```text
ObservedCustomerQuerySecurityIT
```

It verifies the presence of the contractual read scope using reflection.

That evidence is useful but insufficient as the sole API-security proof.

It does not execute HTTP requests and therefore does not prove:

- HTTP 403 behavior;
- request-header validation;
- controller invocation boundaries;
- response correlation header;
- request validation behavior.

### 8.2.1 addition

The lot adds:

```text
ObservedCustomerQueryControllerTest
```

using the golden-module pattern:

```text
@WebMvcTest
MockMvc
@EnableMethodSecurity
@WithMockUser
MockitoBean boundaries
```

Covered behavioral cases:

- published `observed-customer.read` scope is accepted;
- response echoes `X-Correlation-ID`;
- authenticated caller without the scope receives 403;
- missing correlation header receives 400;
- malformed correlation UUID receives 400;
- page size above 200 receives 400;
- detail endpoint rejects caller without read scope;
- payment-history endpoint rejects caller without read scope.

Status after applying the 8.2.1 file:

```text
COVERED for controller/security baseline
```

Detailed mapper and ProblemDetail behavior remains covered by dedicated focused
tests/conformance evidence rather than being embedded into one controller test.

---

## 6. Infrastructure coverage

Direct repository evidence already exists for banking integration, including:

```text
AmplitudeCustomerVerificationClientMockWebServerTest
BankingVerificationPropertiesTest
AmplitudeResponseValidatorTest
BankingVerificationErrorClassifierTest
CoreBankingHttpIntegrationTest
RetryingBankingCustomerVerificationAdapterTest
```

This provides meaningful coverage for the Verification external-adapter side.

The Observation side contains production adapters for:

```text
JpaObservedCustomerRepositoryAdapter
JpaObservedPaymentRepositoryAdapter
JpaObservedCustomerQueryAdapter
JpaObservedCustomerPaymentQueryAdapter
ObservedCustomerPersistenceMapper
HmacObservedCustomerCursorCodec
```

The authoritative branch also contains contract/architecture checks around
cursor authentication, masking and persistence wiring.

However, 8.2.1 does not assert a new PostgreSQL persistence test is missing
without positive evidence of absence.

Status:

```text
PARTIAL / VERIFY EXISTING POSTGRESQL BEHAVIORAL IT
```

This status is intentionally conservative.

A new persistence integration test SHALL only be added after confirming that
the current repository does not already contain equivalent behavioral coverage.

---

## 7. Customer status after 8.2.1

| Dimension | Status | Main evidence |
|---|---|---|
| Domain | COVERED | `ObservedCustomerTest` and existing verification-domain tests |
| Application | COVERED | `ObservedCustomerQueryServiceTest` and existing verification application tests |
| API | COVERED after 8.2.1 addition | `ObservedCustomerQueryControllerTest`, readiness contract/security tests |
| Infrastructure — Banking | COVERED | MockWebServer, classifier, validator, retry and Core Banking integration tests |
| Infrastructure — Observation persistence/query | PARTIAL | architecture/conformance present; PostgreSQL behavioral coverage must be positively verified |

Overall:

```text
CUSTOMER = PARTIAL
```

The remaining partial status is limited to proving Observation persistence/query
behavioral integration coverage; it is not a request to regenerate Customer.

---

## 8. Validation commands

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress \
    -pl customer -am test

mvn --batch-mode --no-transfer-progress \
    -pl customer -am clean verify

mvn --batch-mode --no-transfer-progress \
    -pl customer -am -Pfull-tests clean verify
```

The final command requires Docker when Testcontainers-backed integration tests
are present.

---

## 9. 8.2.1 rule

Do not create:

```text
CustomerTestEverything
ObservedCustomerFullSpringTest
```

to close the remaining persistence status.

The next persistence decision must first identify existing PostgreSQL tests and
then add only the smallest missing focused integration test.

Consistency with `partner` takes precedence over increasing test count.
