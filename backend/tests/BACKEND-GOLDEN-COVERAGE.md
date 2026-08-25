# SIXPAY CONNECT — Backend Golden Test Coverage

## Lot

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.0 — Coverage inventory & golden conformance
```

## 1. Purpose

This document records the evidence-backed backend test coverage inventory for
SIXPAY CONNECT against the `partner` golden-module reference.

Authoritative implementation branch:

```text
feat/repository-baseline-consolidation
```

The goal of 8.2.0 is classification and gap identification.

It SHALL NOT introduce cross-module tests, move existing tests between modules,
or create broad `@SpringBootTest` suites that duplicate focused layer tests.

The `partner` module remains the golden business-module reference.

---

## 2. Golden conformance model

The golden module defines four independent coverage dimensions.

### Domain

Validate, where applicable:

- invariants;
- legal transitions;
- illegal transitions;
- value objects;
- domain policies;
- deterministic calculations;
- terminal-state protection.

### Application

Validate, where applicable:

- happy paths;
- rejected operations;
- output-port interaction;
- dependency failures;
- orchestration decisions;
- boundary and edge cases.

### API

Validate only when the module exposes an HTTP API:

- HTTP status;
- request and response payloads;
- Bean Validation;
- RBAC;
- scopes;
- object-level authorization;
- error mapping;
- correlation behavior.

### Infrastructure

Validate, where applicable:

- mappings;
- persistence;
- database constraints;
- ordering;
- pagination/cursors;
- optimistic locking or persistence conflicts;
- external-adapter mappings;
- technical failure classification.

The four dimensions are separate. A single test SHALL NOT be used as a
replacement for focused tests at all four layers.

---

## 3. Status vocabulary

| Status | Meaning |
|---|---|
| `COVERED` | Direct repository evidence demonstrates focused automated coverage for the dimension. |
| `PARTIAL` | Direct tests exist, but important implemented behavior still requires a focused coverage review. |
| `MISSING` | The production responsibility is confirmed and the corresponding test coverage is confirmed absent. |
| `N/A` | The dimension does not apply to the implemented module responsibility. |
| `UNVERIFIED` | Current repository evidence is insufficient to assert COVERED, PARTIAL or MISSING without guessing. |

`UNVERIFIED` is intentionally different from `MISSING`.

A later 8.2.x module review SHALL resolve every `UNVERIFIED` status before
Phase 8.2 closure.

---

## 4. Golden reference — partner

The `partner` README defines the canonical minimum evidence:

```text
PartnerTest
PartnerApplicationServiceTest
PartnerControllerTest
PartnerPersistenceIT
```

It also references focused tests for individual capabilities and requires
coverage of:

```text
Domain
Application
secured API
PostgreSQL persistence
```

The accepted structural model is therefore:

```text
src/test/java/com/sixpay/<module>/
├── api/
├── application/
│   └── service/
├── domain/
└── infrastructure/
    └── persistence/
```

Only directories containing real tests are required.

### Golden rule

A test such as:

```text
PaymentTestEverything
```

combining full Spring Boot startup, HTTP, database and extensive internal
Mockito stubbing is not a SIXPAY golden-module pattern.

Tests SHALL be implemented at the lowest useful layer.

---

## 5. Evidence-backed module matrix

| Module | Domain | Application | API | Infrastructure | 8.2 assessment |
|---|---|---|---|---|---|
| `partner` | COVERED | COVERED | COVERED | COVERED | GOLDEN |
| `customer` | UNVERIFIED | PARTIAL | PARTIAL | PARTIAL | Review gaps only; significant test estate already exists |
| `payment` | COVERED | PARTIAL | PARTIAL | PARTIAL | Preserve domain tests; focus on post-domain layers |
| `accounting` | PARTIAL | COVERED | N/A | PARTIAL | Main visible gap: behavioral persistence coverage |
| `reporting` | UNVERIFIED | PARTIAL | PARTIAL | PARTIAL | Query/security evidence exists; module-local completeness must be checked |
| `notification` | UNVERIFIED | UNVERIFIED | N/A | UNVERIFIED | Application-rich module; dedicated inventory required |
| `administration` | N/A / UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | Apply golden model only to implemented backend capabilities |
| `security` | N/A | PARTIAL | PARTIAL | PARTIAL | Platform module; assess by authentication/authorization responsibility |

No `MISSING` status is asserted in 8.2.0 unless absence is positively verified.
This prevents creating duplicate tests from an incomplete directory listing.

---

## 6. Customer evidence

The authoritative branch contains substantial Customer testing evidence.

Observed test assets include:

```text
CustomerArchitectureTest
ObservedCustomerClockWiringArchitectureTest
ObservedCustomerPersistenceClockWiringArchitectureTest
ObservedCustomerQueryContractConformanceTest
ObservedCustomerQueryLot477ApplicationTest

AmplitudeCustomerVerificationClientMockWebServerTest
BankingVerificationPropertiesTest
AmplitudeResponseValidatorTest
BankingVerificationErrorClassifierTest
CoreBankingHttpIntegrationTest
RetryingBankingCustomerVerificationAdapterTest
```

Bootstrap/readiness evidence also exists for Observed Customer contract and
security behavior.

### 8.2.1 must resolve

Domain:

- observed-customer invariants;
- immutable identifiers/value objects;
- verification-domain decisions.

Application:

- search happy path;
- detail happy path;
- payment-history happy path;
- not found;
- invalid cursor;
- unavailable projection;
- dependency failures.

API:

- search;
- detail;
- payment list;
- validation;
- RBAC/scopes;
- masking;
- ProblemDetail;
- correlation;
- cursor/page validation.

Infrastructure:

- cursor codec;
- query adapters;
- persistence;
- provider mapping;
- timeout/retry classification.

8.2.1 SHALL reuse existing tests and add only confirmed missing cases.

---

## 7. Customer-owned subscription capability

The former `backend/subscription` entry was an empty reactor placeholder, not
an implemented bounded context. FS-2.9 removes it. Customer enrollment and the
implemented partner-subscription lifecycle remain owned and covered by
`backend/customer`.

---

## 8. Payment evidence

The Payment module documentation establishes a rich, already-validated domain:

```text
1 aggregate root
17 named operations
17 states
38 legal transitions
33 explicit domain events
14 pure policies
4 pure domain services
```

The domain layer is therefore not a target for blanket regeneration.

### 8.2.3 focus

Application/API/query work introduced after the domain kernel must be checked
independently for:

- query happy paths;
- rejection/not-found paths;
- access-policy behavior;
- list/detail semantics;
- page/cursor validation;
- error mapping;
- correlation;
- projection mapping;
- stable ordering;
- persistence/query adapter behavior.

Existing domain tests SHALL be preserved.

---

## 9. Accounting evidence

Direct repository evidence includes:

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

This demonstrates meaningful focused application, policy and adapter coverage.

### Confirmed 8.2.4 review target

The module contains persistence adapters/entities and database migrations.

Architecture tests are not sufficient proof of runtime persistence behavior.

8.2.4 must verify focused behavioral coverage for:

- AccountingBatch persistence round-trip;
- batch items;
- tracking persistence;
- deterministic retrieval;
- persistence conflict semantics;
- database constraints;
- applicable transaction/locking behavior.

The API dimension is `N/A` unless a module-owned inbound HTTP controller is
confirmed.

---

## 10. Reporting inventory gate

Reporting owns Payment Audit querying responsibilities.

Existing repository evidence includes bootstrap-level contract/security
readiness tests for Payment Audit.

These are complementary evidence and do not automatically prove complete
module-local coverage.

8.2.5 must verify focused tests for:

```text
Application
  payment timeline
  audit search
  audit detail
  export creation
  export status

API
  HTTP status
  payload mapping
  search validation
  reporting scopes
  RBAC
  ProblemDetail
  correlation

Infrastructure
  audit projection
  deterministic ordering
  cursor/page behavior
  export persistence
  not-found behavior
```

---

## 11. Notification inventory gate

Notification contains operational orchestration and delivery responsibilities.

The golden model applies to implemented responsibilities, but an inbound REST
API must not be invented solely to satisfy the matrix.

API is therefore `N/A` unless a module-owned HTTP controller exists.

8.2.6 must verify focused coverage for:

```text
registration/planning
delivery success
retryable failure
permanent failure
replay
retention/purge
operations orchestration
template mapping
recipient resolution
persistence
delivery gateway behavior
```

Retry tests must distinguish:

```text
known retryable failure
known permanent failure
successful delivery
```

---

## 12. Administration inventory gate

Administration must not receive artificial domain objects, controllers or
persistence solely to imitate `partner`.

8.2.7 must first identify implemented backend responsibilities.

For each implemented responsibility, apply the golden layer that actually
exists.

If no meaningful domain model exists, Domain is `N/A`.

If no module-owned HTTP API exists, API is `N/A`.

---

## 13. Security inventory gate

Security is a platform module, not a standard business bounded context.

The golden model therefore applies by responsibility.

Existing branch evidence includes focused tests around local authentication
and authorities.

8.2.8 must verify:

### Authentication

- valid credentials;
- invalid credentials;
- password encoding;
- principal mapping;
- local authentication mode.

### Authorization

- role mapping;
- scope/authority mapping;
- JWT authority conversion;
- local-user authorities.

### API

When local administration/authentication endpoints are enabled:

- HTTP success;
- invalid input;
- unauthenticated behavior;
- forbidden behavior;
- response safety.

### Infrastructure

- local-user persistence;
- unique identities;
- security migrations;
- credential storage;
- OIDC/local-mode configuration boundaries.

Domain is `N/A` unless a true domain responsibility is present.

---

## 14. Conformance rules for all 8.2.x lots

Each module review SHALL follow this order:

```text
1. inspect production responsibilities
2. inventory existing tests
3. map tests to golden dimensions
4. identify behavior gaps
5. prove that a gap is real
6. generate only focused missing tests
7. run module-local tests
8. run applicable integration tests
9. update this inventory
```

Do not start at step 6.

### Lowest useful layer rule

```text
domain invariant
  -> domain unit test

application rejection
  -> application service unit test

HTTP authorization
  -> WebMvc/API test

database constraint
  -> PostgreSQL integration test

cross-module workflow
  -> Phase 8.3, not Phase 8.2
```

---

## 15. Cross-module boundary

Phase 8.2 SHALL keep tests with their owning module.

Tests requiring several bounded contexts or the assembled backend belong to:

```text
backend/tests
```

and are implemented in Phase 8.3.

Existing bootstrap readiness/architecture tests may remain where currently
owned; 8.2.0 does not relocate them.

---

## 16. 8.2.0 exit criteria

8.2.0 is complete when:

- the golden coverage dimensions are fixed;
- current evidence is mapped without inventing absence;
- `partner` remains the reference;
- every target module has an explicit review status;
- known gaps are separated from unverified gaps;
- no production code has been changed;
- no existing test has been moved;
- no duplicate test has been generated;
- follow-up module reviews are ordered and scoped.

The remaining `UNVERIFIED` entries are intentionally resolved by:

```text
8.2.1 Customer
8.2.2 Historical standalone Subscription assessment (absorbed by FS-2.9)
8.2.3 Payment
8.2.4 Accounting
8.2.5 Reporting
8.2.6 Notification
8.2.7 Administration
8.2.8 Security
8.2.9 Final golden coverage closure
```
