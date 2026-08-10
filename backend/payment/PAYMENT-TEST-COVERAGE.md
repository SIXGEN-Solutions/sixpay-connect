# SIXPAY CONNECT — Payment Golden Test Coverage

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.3 — Payment
8.2.9 remediation — Application and Infrastructure closure
```

## 1. Golden reference

`partner` remains the golden business-module reference.

Payment is assessed independently across:

```text
Domain
Application
API
Infrastructure
```

The goal is focused behavioral evidence, not test-count inflation.

## 2. Final classification

```text
DOMAIN          = COVERED
APPLICATION     = COVERED
API             = COVERED
INFRASTRUCTURE  = COVERED
```

Overall:

```text
PAYMENT = COVERED
```

## 3. Domain

The existing Payment domain suite remains authoritative for:

```text
invariants
legal and illegal transitions
terminal-state behavior
replay semantics
version/timestamp rules
domain-event ordering
policy/domain-service behavior
```

No new domain test is introduced by 8.2.9.

## 4. Application closure

### Existing evidence

Existing focused tests already include:

```text
PaymentMutationCoordinatorTest
PaymentReconciliationServiceTest
PaymentAccessPolicyTest
PaymentApplicationLayerArchitectureTest
```

`PaymentMutationCoordinatorTest` proves:

```text
new aggregate persistence
mutation persistence
no-op mutation without side effects
missing Payment rejection
changed aggregate requires domain events
safe outbox staging timestamp
```

`PaymentAccessPolicyTest` proves:

```text
Partner subject-bound search visibility
object-level Partner isolation
fail-closed ownership
role/authority rejection for operations
```

### Added evidence

8.2.9 adds:

```text
SecuredPaymentProjectionQueryServiceTest
SearchPaymentProjectionsQueryTest
```

The secured query service test proves:

```text
search visibility is resolved before projection search
detail access descriptor is resolved before detail read
object access policy is enforced before projection read
missing access descriptor fails closed
policy rejection prevents projection read
authorized missing projection remains Optional.empty
null Payment identifier is rejected before infrastructure access
```

The query-value test proves:

```text
default sort
page-size bounds
created-at range validation
non-negative amount bounds
amountMin <= amountMax
```

Application status:

```text
APPLICATION = COVERED
```

## 5. API

Existing Payment Query WebMVC evidence covers:

```text
search 200
detail 200
correlation header
request validation
403 policy rejection
404 unknown Payment
Spring Boot 4 method validation error mapping
```

Status:

```text
API = COVERED
```

## 6. Infrastructure — positive verification

8.2.9 does not add a redundant generic `PaymentPersistenceIT`.

The authoritative branch already contains focused behavioral evidence.

### Projection/query

```text
PaymentProjectionAdaptersIT
PaymentProjectionCursorCodecTest
```

These prove:

```text
PostgreSQL-backed projection search
stable created-at ordering
keyset/cursor pagination
detail projection mapping
masked account output
banking/posting/TFJ mapping
Partner search fail-closed
cursor round-trip
cursor sort mismatch rejection
malformed cursor rejection
```

### Idempotency

```text
PaymentIdempotencyFoundationIT
PaymentIdempotencyReplayStoreTest
PaymentIdempotencyHasherTest
```

These prove:

```text
completed-result replay
same key + different request conflict
serialized concurrent use of one idempotency key
hash/replay-store semantics
```

### Persistence schema and repository

```text
PaymentPersistenceMigrationIT
PaymentRepositoryAdapterTest
PaymentPersistenceArchitectureTest
```

These prove:

```text
Flyway migration execution
payment table creation
required unique source/external-reference constraint
repository mapping boundary
optimistic-locking design through JPA @Version
```

The production repository translates optimistic-lock and database constraint
failures into `PaymentPersistenceException`.

### Atomic state/audit/outbox

```text
PaymentAuditAtomicityIT
PaymentOutboxAtomicityIT
PaymentEndToEndIntegrationIT
```

These prove:

```text
Payment + audit commit/rollback atomicity
Payment + outbox commit/rollback atomicity
PostgreSQL outbox persistence
integration-envelope ordering
Amplitude posting boundary
Accounting/Notification event routing probes
correlation propagation
```

### Provider-specific adapters

Payment keeps Amplitude/provider logic in the owning module and provider-neutral
concerns in `backend/integration`, per repository rules.

Architecture and module-level integration tests already validate that boundary;
Phase 8.3 remains the owner of broader cross-module scenarios.

Infrastructure status:

```text
INFRASTRUCTURE = COVERED
```

## 7. Why no new generic persistence IT is added

The previous `PARTIAL` status was a verification gap, not proof that the
infrastructure had no behavioral tests.

Adding another all-purpose persistence test would duplicate:

```text
PaymentProjectionAdaptersIT
PaymentIdempotencyFoundationIT
PaymentAuditAtomicityIT
PaymentOutboxAtomicityIT
PaymentPersistenceMigrationIT
PaymentEndToEndIntegrationIT
```

and violate the golden layered-test rule.

## 8. Validation

From `backend/`:

```bash
mvn -pl payment \
    -Dtest=SecuredPaymentProjectionQueryServiceTest,SearchPaymentProjectionsQueryTest \
    test
```

Then:

```bash
mvn -pl payment -am test
```

For PostgreSQL/full integration evidence:

```bash
mvn -pl payment -am \
    -Pfull-tests clean verify
```

Finally:

```bash
mvn -pl tests \
    -Dtest=BackendGoldenCoverageGateTest \
    test
```

## 9. Exit decision

Payment's 8.2.9 blocker is resolved when:

```text
new Application tests = GREEN
existing Payment module tests = GREEN
full-tests = GREEN
PAYMENT-TEST-COVERAGE.md contains no PARTIAL marker
```
