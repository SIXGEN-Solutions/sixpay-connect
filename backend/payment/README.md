# SIXPAY CONNECT — Payment Module

## Status

```text
Current increment: Lot 3.1 — Payment Module Foundation
Implementation scope: PAYMENT_DOMAIN_ONLY
Global generation: FORBIDDEN
Current increment generation: AUTHORIZED
```

## Responsibility

The `payment` module owns the Payment domain model.

Lot 3.1 establishes only the module foundation. It does not implement the
Aggregate Root, Value Objects, snapshots, policies, Domain Services or domain
events yet.

## Platform dependencies

| Module | Reason |
| --- | --- |
| `common` | Correlation and other approved technical contracts |
| `shared-kernel` | AggregateRoot, DomainEvent, DomainException, Money and ValueObject |
| `junit-jupiter` | Unit and architecture tests only |

No Spring starter, persistence library, integration client or business-domain
module is introduced.

## Current production sources

```text
src/main/java/com/sixpay/payment/
├── PaymentModule.java
└── domain/
    └── package-info.java
```

## Current tests

```text
src/test/java/com/sixpay/payment/architecture/
└── PaymentArchitectureTest.java
```

The architecture test verifies:

- the approved foundation sources;
- dependency minimality;
- framework independence of the domain;
- absence of cross-domain imports;
- absence of an executable Spring Boot application;
- the domain-only implementation authorization;
- reuse of shared platform primitives.

## Build

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress -pl payment -am test
```

The repository-wide CI remains:

```bash
mvn --batch-mode --no-transfer-progress clean verify
```

## Next increment

```text
Lot 3.2 — Identifiers, Value Objects and classifications
```

Lot 3.2 requires explicit activation in the Payment AI context manifest before
new domain classes are generated.
