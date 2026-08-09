# SIXPAY CONNECT — Reporting Module

## Purpose

The Reporting module owns immutable Payment audit querying and controlled audit export.

The `partner` module remains the golden business-module reference for implementation
structure and testing discipline.

## Current responsibilities

```text
Domain
  audit classifications and policies

Application
  Payment timeline query
  Payment audit search/detail
  controlled audit export
  export idempotency/fingerprinting

API
  GET /internal/api/v1/payments/{paymentId}/timeline
  GET /internal/api/v1/payment-audit-records
  GET /internal/api/v1/payment-audit-records/{auditId}
  POST /internal/api/v1/payment-audit-exports
  GET /internal/api/v1/payment-audit-exports/{exportId}

Infrastructure
  audit projection reads
  authenticated cursors
  audit-access recording
  JDBC export job store
  export generation
  export artifact storage
```

## Security

Read operations require:

```text
SCOPE_payment.audit.read
```

Audit export operations require both:

```text
SCOPE_payment.audit.read
SCOPE_payment.audit.export
```

## Test strategy

Reporting follows the golden layered model:

```text
Domain          -> focused policy/value tests where behavior exists
Application     -> service tests without Spring
API             -> @WebMvcTest + MockMvc + method security
Infrastructure  -> focused adapters and PostgreSQL integration tests
```

Existing integration coverage includes persistence, masking and export idempotency.

Phase 8.2.5 adds focused application-query and HTTP-boundary tests rather than
duplicating the existing integration suite.

## Validation

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress -pl reporting -am test
mvn --batch-mode --no-transfer-progress -pl reporting -am clean verify
mvn --batch-mode --no-transfer-progress -pl reporting -am -Pfull-tests clean verify
```

## Phase 8 status

```text
Domain          COVERED
Application     COVERED
API             COVERED
Infrastructure  COVERED
```

Detailed evidence is maintained in:

```text
REPORTING-TEST-COVERAGE.md
```
