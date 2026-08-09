# SIXPAY CONNECT — Payment Module

## Current implementation status

The Payment module has evolved beyond the original domain-only increment.

The authoritative implementation branch is:

```text
feat/sixpay-test-validate-pilote
```

The Payment domain kernel remains the foundation, but the module now also
contains application, API, security and infrastructure responsibilities.

---

## Domain kernel

The Payment domain kernel remains intentionally rich and strongly validated.

Implemented domain program:

```text
1 Payment Aggregate Root
1 immutable PaymentState
17 named operations
17 states
38 legal transitions
33 explicit Payment domain events
14 pure policies
12 immutable policy profiles
4 pure Domain Services
```

### Aggregate ownership

`Payment` is the sole owner of:

- lifecycle transitions;
- business version and timestamps;
- bounded accepted evidence;
- Payment failure and finality;
- ordered domain-event registration.

Each successful operation prepares a complete immutable next state and its
complete event batch before atomic in-memory commit.

An invalid transition, conflict or identical replay leaves state, version and
pending events unchanged.

---

## Event model

Events implement `PaymentDomainEvent`, extending the shared `DomainEvent`.

Every event carries:

```text
eventId
paymentId
paymentReference
correlationId
paymentStatus
aggregateVersion
eventSequence
causationId?
occurredAt
```

Only explicit safe payload records are used.

Full aggregates and snapshots are never event fields.

---

## Current application responsibilities

The module now includes application responsibilities beyond the original
domain-only milestone.

These include, among others:

```text
PaymentProjectionQueryUseCase
SearchPaymentProjectionsQuery
PaymentAccessPolicy
PaymentRolePolicy
PaymentPartnerIsolationPolicy
PaymentAuthority
```

The application layer owns query orchestration and Payment-specific access
policy decisions.

---

## Current internal API

The module exposes the internal Payment query API:

```text
GET /internal/api/v1/payments
GET /internal/api/v1/payments/{paymentId}
```

Implemented controller:

```text
PaymentQueryController
```

The API supports:

- Payment search;
- Payment detail lookup;
- cursor-based pagination;
- page-size validation;
- amount/date/status/reference filters;
- currency validation;
- correlation propagation;
- Payment-specific authorization.

The published contract remains:

```text
documentation/contracts/internal/payment-query-api-v1.yaml
```

---

## Security

Payment authorization is centralized through:

```text
PaymentAccessPolicy
```

The policy combines:

```text
CurrentUserProvider
PaymentRolePolicy
PaymentPartnerIsolationPolicy
PaymentAuthority
```

Payment authorization therefore includes:

- authentication;
- role authorization;
- required authority/scope;
- partner isolation;
- object-level access where applicable.

Published read authority:

```text
SCOPE_payment.read
```

---

## Error handling

The Payment API uses:

```text
PaymentApiExceptionHandler
```

Representative mappings include:

```text
PaymentNotFoundException              -> 404
PaymentAccessDeniedException          -> 403
AuthenticationCredentialsNotFound     -> 401
PaymentQueryRateLimitExceeded         -> 429
PaymentQueryUnavailableException      -> 503
PaymentIdempotencyConflict            -> 409
invalid request / validation errors   -> 400
```

Problem responses preserve correlation identifiers.

---

## Current infrastructure

The module now contains infrastructure responsibilities beyond the original
domain kernel.

These include, where applicable:

- Spring Data JPA;
- PostgreSQL persistence;
- query projections;
- idempotency infrastructure;
- TresorPay integration;
- Core Banking integration;
- resilience and error classification;
- observability;
- Flyway-backed schema evolution.

The owning Payment module retains provider-specific mappings and behavior.

Provider-neutral infrastructure remains in:

```text
backend/integration
```

---

## Test strategy

The `partner` module remains the golden business-module reference.

Payment tests must remain layered and focused.

Expected structure:

```text
backend/payment/src/test/java/com/sixpay/payment/
├── api/
├── application/
├── domain/
└── infrastructure/
```

### Domain

The existing comprehensive domain suite remains authoritative.

Do not duplicate the Payment transition matrix in a new monolithic test.

### Application

Use focused unit tests for:

- query orchestration;
- role/authority policy;
- partner isolation;
- object-level access;
- rejected operations;
- edge cases.

### API

Use focused WebMVC tests for:

- HTTP status;
- payload;
- validation;
- access-policy enforcement;
- error mapping;
- correlation.

### Infrastructure

Use focused integration tests for:

- persistence;
- query mapping;
- ordering;
- cursor pagination;
- database constraints;
- idempotency;
- provider adapter behavior.

PostgreSQL-specific behavior should use Testcontainers where applicable.

---

## Golden rule

Do not create tests such as:

```text
PaymentTestEverything
PaymentFullSpringBootTest
```

that mix:

```text
full Spring Boot
HTTP
database
Mockito internals
```

in one test.

Use the lowest useful testing layer.

Consistency with `partner` takes precedence over convenience.

---

## Validation

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress     -pl payment -am test
```

Then:

```bash
mvn --batch-mode --no-transfer-progress     -pl payment -am clean verify
```

For integration coverage:

```bash
mvn --batch-mode --no-transfer-progress     -pl payment -am -Pfull-tests clean verify
```

Docker is required when Testcontainers-backed tests execute.

---

## Phase 8 status

Current Phase 8 coverage classification:

```text
Domain          COVERED
Application     PARTIAL
API             COVERED
Infrastructure  PARTIAL
```

Detailed Phase 8.2.3 evidence is documented in:

```text
PAYMENT-TEST-COVERAGE.md
```

---

## Final rule

The Payment domain kernel remains the authoritative owner of Payment business
state and transitions.

Application, API and infrastructure layers must orchestrate and expose that
kernel without duplicating its business rules.

Consistency takes precedence over creativity.
