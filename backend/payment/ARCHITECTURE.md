# Architecture Decision Record — Payment Domain Foundation

## Decision

The `payment` module is initialized as a non-executable Maven JAR whose current
authorized implementation scope is limited to the pure Payment domain.

## Authority

This decision implements the validated IA-1 Payment model without changing its
semantics.

The model remains frozen at:

```text
17 states
38 transitions
76 invariants
16 commands
17 aggregate operations
33 domain events
14 policies
4 pure Domain Services
```

## Dependency direction

```text
payment
├── common
└── shared-kernel
```

`payment` does not depend on another business module.

The dependency on `common` is direct because the final model uses approved
cross-cutting contracts such as `CorrelationId`.

The dependency on `shared-kernel` is direct because Payment must reuse:

- `AggregateRoot`;
- `DomainEvent`;
- `DomainException`;
- `Money`;
- `ValueObject`.

## Domain-only boundary

Authorized production path for the Lot 3 program:

```text
backend/payment/src/main/java/com/sixpay/payment/domain/**
```

The module marker is the only production type allowed outside `domain` during
Lot 3.

The following layers remain outside the active authorization:

```text
api
application
configuration
events
infrastructure
```

The top-level `events` package is reserved for integration-event contracts.
Payment domain events will live under `domain/event`.

## Framework independence

The Payment domain must not import:

- Spring;
- Jakarta Persistence or Servlet APIs;
- Hibernate;
- Jackson;
- Payment API, application, configuration, infrastructure or integration-event
  packages;
- another SIXPAY business domain.

## Module assembly

`PaymentModule` is a marker class only.

It is not annotated and does not expose a `main` method. `bootstrap` remains the
only executable Spring Boot application.

## Controlled implementation authorization

The repository retains:

```text
globalCodeGenerationAllowed: false
```

A narrower authorization is active:

```text
scope: PAYMENT_DOMAIN_ONLY
currentIncrement: LOT_3_1_PAYMENT_MODULE_FOUNDATION
currentIncrementCodeGenerationAllowed: true
futureIncrementActivationRequired: true
```

This authorization does not override contract, security, integration or
operations blockers outside the pure domain.

## Consequences

- Lot 3 can implement and test the domain incrementally.
- No API, database schema, adapter or external call is inferred from the domain
  model.
- Each subsequent Lot 3 increment requires explicit activation.
- Architecture tests fail if the active boundary is broadened silently.
