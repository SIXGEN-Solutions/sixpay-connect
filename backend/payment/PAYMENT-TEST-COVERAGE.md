# SIXPAY CONNECT — Payment Golden Test Coverage

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.3 — Payment
```

## 1. Reference

The `partner` module remains the golden business-module reference.

Payment is assessed independently across:

```text
Domain
Application
API
Infrastructure
```

The objective is not to increase test count indiscriminately.

The objective is to preserve the strong existing Payment domain validation and
close only confirmed gaps in newer application/query/API/infrastructure layers.

---

## 2. Documentation synchronization

`backend/payment/README.md` previously described the historical
domain-only increment and listed application handlers, repositories,
controllers and adapters as deliberately absent.

That description was stale relative to the authoritative branch.

Phase 8.2.3 updates the module README while preserving the valid domain-kernel
documentation.

---

## 3. Domain coverage

The Payment domain kernel is already deliberately comprehensive.

Repository documentation records:

```text
1 Payment aggregate root
1 immutable PaymentState
17 named operations
17 states
38 legal transitions
33 explicit Payment domain events
14 pure policies
12 immutable policy profiles
4 pure domain services
```

Existing validation protects:

```text
legal transitions
illegal transitions
terminal-state behavior
identical replay
version/timestamp behavior
ordered event registration
PAY-* traceability
canonical exception package
```

Status:

```text
DOMAIN = COVERED
```

No new monolithic Payment domain test is introduced.

---

## 4. Application coverage

The current branch includes:

```text
PaymentProjectionQueryUseCase
SearchPaymentProjectionsQuery
PaymentAccessPolicy
PaymentRolePolicy
PaymentPartnerIsolationPolicy
PaymentAuthority
```

The application/security layer must be reviewed independently for:

```text
search happy path
detail happy path
projection unavailable
cursor validation
search visibility
object-level access
role + authority combination
partner isolation
edge cases
```

Status:

```text
APPLICATION = PARTIAL
```

No speculative test is added without proving a specific missing behavior.

---

## 5. API coverage

The current Payment Query API exposes:

```text
GET /internal/api/v1/payments
GET /internal/api/v1/payments/{paymentId}
```

Existing readiness evidence includes:

```text
PaymentQueryContractTest
PaymentQuerySecurityIT
```

Those tests validate contract and authority conformance but do not execute the
HTTP boundary.

Phase 8.2.3 adds:

```text
PaymentQueryControllerTest
```

with:

```text
@WebMvcTest
MockMvc
@EnableMethodSecurity
MockitoBean boundaries
```

Covered cases include:

```text
search 200
correlation response header
missing correlation -> 400
invalid correlation UUID -> 400
size > 200 -> 400
invalid currency -> 400
access-policy rejection -> 403
detail 200
unknown payment -> 404
```

Status:

```text
API = COVERED
```

---

## 6. Infrastructure coverage

The Payment POM already includes:

```text
Spring Data JPA
PostgreSQL runtime
Flyway test support
Testcontainers JUnit Jupiter
Testcontainers PostgreSQL
```

No Maven dependency change is required.

Infrastructure review must verify focused behavioral coverage for:

```text
projection persistence
query mapping
stable ordering
cursor pagination
persistence conflicts
masked data
TresorPay mapping
Core Banking behavior
timeout/failure classification
idempotency persistence semantics
```

A new `PaymentPersistenceIT` must not be generated until equivalent existing
coverage is positively ruled out.

Status:

```text
INFRASTRUCTURE = PARTIAL
```

---

## 6.1 Spring Boot 4 method-parameter validation

Spring Boot 4 / Spring MVC raises:

```text
HandlerMethodValidationException
```

for controller method-parameter constraints such as:

```text
@Max
@Min
@Pattern
@Size
@DecimalMin
```

The Payment exception handler now maps this exception to the existing:

```text
400 INVALID_REQUEST
```

problem response.

This matches the golden `partner` error-handling pattern and preserves the
published Payment error envelope instead of accepting Spring MVC's empty
default 400 response.

---

## 7. Current status

| Dimension | Status |
|---|---|
| Domain | COVERED |
| Application | PARTIAL |
| API | COVERED |
| Infrastructure | PARTIAL |

Overall:

```text
PAYMENT = PARTIAL
```

---

## 8. Validation commands

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress     -pl payment -am test
```

```bash
mvn --batch-mode --no-transfer-progress     -pl payment -am clean verify
```

```bash
mvn --batch-mode --no-transfer-progress     -pl payment -am -Pfull-tests clean verify
```

---

## 9. Golden-module rule

Use:

```text
domain invariant
    -> pure domain unit test

application/security policy
    -> focused unit test

HTTP behavior
    -> WebMvc test

database semantics
    -> PostgreSQL integration test

cross-module workflow
    -> Phase 8.3
```

Do not introduce an all-in-one Payment test suite.
