# SIXPAY CONNECT — Payment Domain Generation Brief

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `3.1 — Payment Module Foundation`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `DOMAIN_ONLY_IMPLEMENTATION_AUTHORIZED`  
> **Global code generation:** **FORBIDDEN**  
> **Current increment:** **AUTHORIZED**

## 1. Governing model

The IA-1 Payment model validated in Lot 2.8 remains frozen:

```text
17 states
38 legal transitions
76 invariants
16 commands
17 aggregate operations
33 domain events
14 policies
4 pure Domain Services
174 named acceptance scenarios
```

Lot 3 translates that model into Java 21 without changing its semantics.

## 2. Authorization

The user explicitly authorizes implementation of the pure Payment domain.

The authorization is recorded as:

```text
PAY-AUTH-IA1-001
scope: PAYMENT_DOMAIN_ONLY
currentIncrement: LOT_3_1_PAYMENT_MODULE_FOUNDATION
currentIncrementCodeGenerationAllowed: true
futureIncrementActivationRequired: true
```

This is not a global generation approval.

The existing contract, security, operations and integration blockers remain
applicable outside the pure domain.

## 3. Lot 3.1 allowed changes

```text
backend/payment/pom.xml
backend/payment/README.md
backend/payment/ARCHITECTURE.md
backend/payment/ACCEPTANCE-TRACEABILITY.md
backend/payment/src/main/java/com/sixpay/payment/PaymentModule.java
backend/payment/src/main/java/com/sixpay/payment/domain/package-info.java
backend/payment/src/test/java/com/sixpay/payment/architecture/PaymentArchitectureTest.java
documentation/ai/payment/PAYMENT_DOMAIN_GENERATION_BRIEF.md
documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml
```

## 4. Lot 3.1 implementation

The Payment module now:

- remains a non-executable JAR;
- depends directly on `common` and `shared-kernel`;
- uses BOM-managed versions;
- introduces JUnit only for tests;
- exposes a framework-free module marker;
- establishes the pure `domain` package;
- enforces the active authorization with architecture tests.

## 5. Reused platform contracts

Payment must reuse:

```text
common
└── CorrelationId and approved cross-cutting contracts

shared-kernel
├── AggregateRoot
├── DomainEvent
├── DomainException
├── Money
└── ValueObject
```

Lot 3 must not create local replacements for these types.

## 6. Explicitly forbidden in Lot 3.1

```text
application services
commands and handlers
REST API
OpenAPI changes
Spring configuration
JPA entities and repositories
database migrations
Outbox implementation
Kafka integration
Amplitude adapters
Notification delivery
bank-specific configuration
semantic changes to the IA-1 model
```

## 7. Activation rule for the next increments

The overall program path is limited to:

```text
backend/payment/src/main/java/com/sixpay/payment/domain/**
backend/payment/src/test/java/com/sixpay/payment/domain/**
backend/payment/src/test/java/com/sixpay/payment/architecture/**
```

However, each next increment requires explicit activation before code is
generated.

The next candidate is:

```text
Lot 3.2 — Identifiers, Value Objects and classifications
```

## 8. Exit criteria

- Payment POM aligns with the Golden Module conventions.
- `PaymentModule` is framework-free and non-executable.
- The pure domain package exists.
- No other production layer is generated.
- Architecture tests enforce dependencies and boundaries.
- The global generation flag remains false.
- Lot 3.1 is the only active implementation increment.

## 9. Verdict

```text
LOT 3.1 PAYMENT MODULE FOUNDATION: IMPLEMENTED
DOMAIN-ONLY PROGRAM AUTHORIZATION: ACTIVE
CURRENT INCREMENT GENERATION: AUTHORIZED
GLOBAL CODE GENERATION: FORBIDDEN
MODEL SEMANTICS: UNCHANGED
NEXT: LOT 3.2 EXPLICIT ACTIVATION
```
