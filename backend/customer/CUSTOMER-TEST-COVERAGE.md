# SIXPAY CONNECT — Customer Golden Test Coverage

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.1 — Customer
8.2.9 remediation — Observation persistence/query closure
```

## Golden reference

`partner` remains the golden business-module reference.

The Customer module continues to keep tests with the owning code and keeps
cross-module scenarios for Phase 8.3.

## Current classification

```text
Domain                                  COVERED
Application                             COVERED
API                                     COVERED
Infrastructure — Banking                COVERED
Infrastructure — Observation persistence/query
                                        COVERED after 8.2.9 remediation
```

Overall:

```text
CUSTOMER = COVERED
```

## Observation persistence/query evidence

Production responsibilities are split between write-side persistence and
read-side query adapters.

Write-side:

```text
JpaObservedCustomerRepositoryAdapter
JpaObservedPaymentRepositoryAdapter
ObservedCustomerPersistenceMapper
ObservedCustomerDataProtector
```

Read-side:

```text
JpaObservedCustomerQueryAdapter
JpaObservedCustomerPaymentQueryAdapter
ObservedCustomerQueryRowMapper
```

The query adapters execute native PostgreSQL/JPA queries and implement stable
keyset pagination.

## Added behavioral PostgreSQL integration test

8.2.9 adds:

```text
ObservedCustomerPersistenceQueryIT
```

The test uses:

```text
PostgreSQL Testcontainers
Flyway
Spring Data JPA
actual persistence adapters
actual persistence mapper
actual data protector
actual query adapters
actual query row mapper
```

It does not start the HTTP layer or banking integration.

Covered behavior:

```text
customer write + reload
protected-NIU lookup
payment/event persistence
customer search through native query adapter
stable customer-page boundary
payment ordering newest first
payment keyset pagination
payment status filtering
payment date filtering
detail query
institution/account reconstruction
```

This closes the specific Phase 8.2.9 blocker that previously read:

```text
Infrastructure — Observation persistence/query = PARTIAL
```

## Test isolation

The test deliberately assembles only Observation persistence/query
responsibilities.

It does not import the whole Customer module and therefore does not pull:

```text
banking HTTP clients
OAuth2 client configuration
Observed Customer HTTP API
audit filters
business security
cross-module Payment integration
```

This follows the golden rule:

```text
one test = one responsibility
```

## Maven infrastructure

The existing Customer POM already contains:

```text
Spring Data JPA
PostgreSQL runtime driver
Spring Boot Flyway test support
Flyway PostgreSQL
Testcontainers JUnit Jupiter
Testcontainers PostgreSQL
```

No POM change is required.

## Validation

From `backend/`:

```bash
mvn -pl customer     -Dtest=ObservedCustomerPersistenceQueryIT     test
```

Then:

```bash
mvn -pl customer -am test
```

Full backend/module integration validation:

```bash
mvn -pl customer -am     -Pfull-tests clean verify
```

Finally:

```bash
mvn -pl tests     -Dtest=BackendGoldenCoverageGateTest     test
```

## Exit decision

The Customer 8.2.9 blocker is resolved when:

```text
ObservedCustomerPersistenceQueryIT = GREEN
customer module tests = GREEN
CUSTOMER-TEST-COVERAGE.md contains no PARTIAL marker
```
