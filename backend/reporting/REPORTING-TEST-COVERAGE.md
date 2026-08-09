# SIXPAY CONNECT — Reporting Golden Test Coverage

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.5 — Reporting
```

## Golden reference

The `partner` module remains the golden reference. Reporting is assessed independently
across Domain, Application, API and Infrastructure.

## Existing evidence

The authoritative branch already contains:

```text
PaymentAuditExportServiceTest
HmacAuditCursorCodecTest
PaymentAuditExportIdempotencyIT
PaymentAuditMaskingIT
PaymentAuditPersistenceIT

ControlledAuditExportArchitectureTest
PaymentAuditQueryArchitectureTest
ReportingFoundationArchitectureTest
```

Bootstrap readiness also contains:

```text
PaymentAuditQueryContractTest
PaymentAuditSecurityIT
```

These are useful but do not replace focused application-service and HTTP-boundary tests.

## Domain

Reporting uses explicit enums/value objects and `AuditEvidencePolicy`.
The integration and service tests exercise the real domain vocabulary and masking rules.

Status:

```text
DOMAIN = COVERED
```

## Application

The branch contains:

```text
PaymentAuditQueryService
PaymentAuditExportService
```

`PaymentAuditExportServiceTest` already covers export behavior.

Phase 8.2.5 adds `PaymentAuditQueryServiceTest` to cover:

```text
timeline happy path
timeline Payment-not-found
timeline pagination cursor creation
search happy path
detail not-found
unexpected repository failure -> PaymentAuditQueryUnavailableException
```

Status:

```text
APPLICATION = COVERED
```

## API

The branch exposes five internal endpoints through:

```text
PaymentAuditQueryController
PaymentAuditExportController
```

Phase 8.2.5 adds focused WebMVC tests for:

```text
timeline success + correlation
read-scope enforcement
method-parameter validation
export read+export double-scope enforcement
export status lookup
```

The exception handler is aligned with Spring Boot 4 method-parameter validation by
mapping `HandlerMethodValidationException` to the existing
`400 PAYMENT_AUDIT_QUERY_INVALID` problem response.

Status:

```text
API = COVERED
```

## Infrastructure

Existing focused evidence already includes:

```text
HmacAuditCursorCodecTest
PaymentAuditPersistenceIT
PaymentAuditMaskingIT
PaymentAuditExportIdempotencyIT
```

These validate authenticated cursor behavior, PostgreSQL audit persistence,
masking and export idempotency.

Status:

```text
INFRASTRUCTURE = COVERED
```




## PostgreSQL TIMESTAMPTZ read conversion

The same integration execution confirmed the inverse JDBC compatibility issue
on reads.

PostgreSQL JDBC 42.7.x in this runtime does not support:

```java
rs.getObject("occurred_at", Instant.class)
```

for `TIMESTAMPTZ`.

Reporting now reads `TIMESTAMPTZ` as `OffsetDateTime` and converts at the JDBC
boundary:

```java
private static Instant instant(
        ResultSet rs,
        String column
) throws SQLException {
    OffsetDateTime value =
            rs.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
}
```

This applies to audit/export timestamp fields while all domain/application
types remain `Instant`.

---

## Production JDBC timestamp binding correction

Phase 8.2.5 integration execution exposed a runtime compatibility issue in the
Reporting JDBC adapters.

Raw `java.time.Instant` values were passed through `MapSqlParameterSource`
without an explicit JDBC-compatible type. With PostgreSQL JDBC 42.7.x in this
runtime, those values are not inferred for generic named parameters.

The correction is applied at the JDBC boundary only:

```java
private static Timestamp timestamp(Instant value) {
    return value == null ? null : Timestamp.from(value);
}
```

It is used by:

```text
JdbcAuditExportJobStore
PaymentAuditProjectionReadAdapter
```

for TIMESTAMPTZ-bound parameters such as:

```text
occurredFrom
occurredTo
requestedAt
expiresAt
snapshotAt
lastOccurredAt
```

No domain or application API changes are introduced.

---

## Phase 8.2.5 integration-test corrections

The existing Reporting integration tests exposed two fixture issues during
Phase 8 validation.

### Export migration dependency

`V202608072120__create_reporting_audit_export.sql` alters
`reporting_payment_audit_evidence`, which is created by
`V202608072058__create_reporting_payment_audit_projection.sql`.

`PaymentAuditExportIdempotencyIT` therefore executes the two migrations in
their real dependency order rather than executing V2120 in isolation.

### PostgreSQL Instant binding

The PostgreSQL JDBC driver does not infer a SQL type for a raw
`java.time.Instant` passed through the generic `JdbcTemplate.update(...)`
argument setter in this test.

`PaymentAuditPersistenceIT` binds the TIMESTAMPTZ fixture as:

```java
Timestamp.from(instant)
```

This is a test-fixture/JDBC binding correction; no Reporting production
behavior is changed.

---

## Final classification

| Dimension | Status |
|---|---|
| Domain | COVERED |
| Application | COVERED |
| API | COVERED |
| Infrastructure | COVERED |

```text
REPORTING = COVERED
```

Cross-module Payment -> Reporting ingestion/consistency scenarios remain Phase 8.3 work.
