# Phase 6 — Lot 6.3 Reporting Foundation

## Purpose

Create the Reporting business-domain foundation that will own the privileged
Payment Audit Query capability.

The module follows the Partner golden-module conventions and remains a standard
non-executable JAR. `bootstrap` remains the only executable composition root.

## Ownership

The authoritative contract declares:

- domain: `reporting`;
- business owner: `reporting`;
- capability: `PAYMENT_AUDIT_QUERY`;
- classification: `RESTRICTED`;
- evidence owners: Payment, Customer, Accounting, Notification, Integration.

Evidence ownership does not authorize Reporting to import or load aggregates
from those domains. Reporting consumes normalized evidence through ports,
projections or approved in-process adapters.

## Foundation structure

```text
backend/reporting/
└── src/main/java/com/sixpay/reporting/
    ├── ReportingModule.java
    ├── api/
    │   ├── controller/
    │   ├── dto/
    │   ├── mapper/
    │   └── exception/
    ├── application/
    │   ├── port/
    │   │   ├── input/
    │   │   └── output/
    │   ├── query/
    │   └── service/
    ├── configuration/
    ├── domain/
    │   ├── model/
    │   ├── policy/
    │   └── exception/
    ├── events/
    └── infrastructure/
        ├── persistence/
        ├── query/
        └── export/
```

## Maven foundation

The Reporting POM is aligned with the Partner golden module for:

- common/shared-kernel/security;
- Spring WebMVC;
- Jakarta validation;
- JPA;
- Spring Security;
- Actuator;
- PostgreSQL runtime;
- OpenAPI annotations;
- Flyway test support;
- Spring/Testcontainers test support.

The Reporting module does not apply the Spring Boot Maven plugin.

## Bootstrap

`bootstrap` depends on `reporting` and remains responsible for executable
packaging and centralized production Flyway execution.

## Deliberate exclusions

Lot 6.3 does not implement:

- Payment timeline REST endpoints;
- audit-record REST endpoints;
- audit export REST endpoints;
- audit persistence schema;
- concrete evidence adapters;
- export jobs.

Those belong to Lots 6.4 and 6.5.

## Exit criteria

```text
P6-003_FOUNDATION = CLOSED
REPORTING_MODULE = STRUCTURALLY_READY
PAYMENT_AUDIT_QUERY = NOT_YET_IMPLEMENTED
NEXT_LOT = 6.4_PAYMENT_TIMELINE_AND_AUDIT_QUERY
```
