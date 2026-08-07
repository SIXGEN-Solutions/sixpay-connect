# Phase 6 — Lot 6.0 Baseline & Contract Conformance

## 1. Purpose

This document establishes the implementation baseline for Phase 6 — Internal tracking/query APIs.

Authoritative contracts:

- `documentation/contracts/internal/payment-query-api-v1.yaml`
- `documentation/contracts/internal/observed-customer-query-api-v1.yaml`
- `documentation/contracts/internal/payment-audit-query-api-v1.yaml`

The repository remains a modular monolith. Internal business capabilities stay inside their owning modules. The `partner` module remains the golden business-module reference. `backend/integration` remains restricted to provider-neutral, cross-cutting integration concerns.

## 2. Contract ownership

| Contract | Capability | Owning module | Current baseline |
|---|---|---|---|
| `payment-query-api-v1.yaml` | Payment Query | `backend/payment` | Implemented, partial conformance |
| `observed-customer-query-api-v1.yaml` | Observed Customer Query | `backend/customer` | Implemented, partial conformance |
| `payment-audit-query-api-v1.yaml` | Payment Audit Query | `backend/reporting` | Contract only; implementation missing |

All three contracts are `ACTIVE_MVP` but currently declare `approvalStatus: PENDING_APPROVAL`.

## 3. Payment Query conformance

### Contract operations

- `GET /internal/api/v1/payments`
- `GET /internal/api/v1/payments/{paymentId}`

### Implementation trace

```text
payment-query-api-v1.yaml
        ↓
PaymentQueryController
        ↓
PaymentProjectionQueryUseCase
        ↓
SecuredPaymentProjectionQueryService
        ↓
PaymentProjectionReadPort
        ↓
PaymentProjectionReadAdapter
        ↓
Payment read projection / PostgreSQL
        ↓
PaymentAccessPolicy
        ↓
Payment architecture / projection tests
```

### Current evidence

- Controller exists in the `payment` owning module.
- Query use case exposes search and detail lookup.
- Application service applies authorization/visibility before projection access.
- Read port explicitly models masked projection reads.
- Read adapter uses query-oriented persistence and does not reconstruct the Payment Aggregate Root.
- Security uses the `payment.read` scope/policy boundary.
- Architecture tests prevent mutations and aggregate loading from the REST query adapter.

### Gap P6-001 — `observedCustomerId`

The contract exposes `observedCustomerId` as a valid Payment search filter.

The current read adapter short-circuits any query containing `observedCustomerId` and returns an empty page.

**Impact:** the endpoint accepts a contractually valid filter but cannot return matching Payments.

**Target lot:** 6.1 — Payment Query completion.

**Required correction:** implement the projection-side association/filter without introducing a direct dependency from Payment to the Customer domain aggregate.

## 4. Observed Customer Query conformance

### Contract operations

- `GET /internal/api/v1/observed-customers`
- `GET /internal/api/v1/observed-customers/{observedCustomerId}`
- `GET /internal/api/v1/observed-customers/{observedCustomerId}/payments`

### Implementation trace

```text
observed-customer-query-api-v1.yaml
        ↓
ObservedCustomerQueryController
        ↓
SearchObservedCustomersUseCase
GetObservedCustomerUseCase
ListObservedCustomerPaymentsUseCase
        ↓
Observation application query services
        ↓
Observation read ports
        ↓
Persistence/query adapters
        ↓
ObservedCustomer CQRS projection
        ↓
SCOPE_observed-customer.read
        ↓
Customer query / architecture tests
```

### Current evidence

- All three contracted endpoints exist in the `customer` owning module.
- The controller requires `SCOPE_observed-customer.read`.
- Correlation ID propagation is implemented.
- Query use cases are separated by operation.
- Stable pagination infrastructure already exists.
- Query observability is already present.
- The capability follows the approved business-module package structure.

### Gap P6-002 — caller-supplied `snapshotAt`

The contract does not expose `snapshotAt` as a request parameter.

The current controller exposes `snapshotAt` on query endpoints.

**Impact:** implementation exposes behavior beyond the published contract.

**Target lot:** 6.2 — ObservedCustomer Query completion.

**Preferred correction:** keep snapshot creation server-side and encode the stable snapshot in the opaque cursor rather than exposing it as a caller parameter, unless the contract is explicitly revised and approved.

## 5. Payment Audit Query conformance

### Contract operations

- `GET /internal/api/v1/payments/{paymentId}/timeline`
- `GET /internal/api/v1/payment-audit-records`
- `GET /internal/api/v1/payment-audit-records/{auditId}`
- `POST /internal/api/v1/payment-audit-exports`
- `GET /internal/api/v1/payment-audit-exports/{exportId}`

### Ownership

The contract declares domain `reporting`, business owner `reporting`, evidence owners Payment/Customer/Accounting/Notification/Integration, and data classification `RESTRICTED`.

### Current implementation baseline

| Layer | Timeline | Audit records | Audit export |
|---|---|---|---|
| Contract | Present | Present | Present |
| Controller | Missing | Missing | Missing |
| Input port | Missing | Missing | Missing |
| Application service | Missing | Missing | Missing |
| Output port | Missing | Missing | Missing |
| Read adapter | Missing | Missing | Missing |
| Persistence/read projection | Missing | Missing | Missing |
| Security implementation | Missing | Missing | Missing |
| Tests | Missing | Missing | Missing |

### Gap P6-003 — Reporting Audit capability absent

`backend/reporting` currently exists only as a minimal domain module and does not yet implement the Payment Audit Query contract.

**Target lots:** 6.3 through 6.5.

The Reporting implementation must follow the golden module structure and must not become an omnipotent cross-domain service. Evidence from other domains must be consumed through approved ports, projections or normalized events/evidence, not by loading their aggregates.

## 6. Governance gap

### Gap P6-004 — contract approval metadata

The three contracts remain:

```text
lifecycleStatus: ACTIVE_MVP
approvalStatus: PENDING_APPROVAL
```

This baseline does not change the metadata automatically. The approval status must be reconciled with the actual governance decision before Phase 6 acceptance.

**Target lot:** 6.6 / governance gate.

## 7. Automated conformance gap

### Gap P6-005 — end-to-end contract gate

Existing architecture tests protect parts of Payment and Customer, but no single Phase 6 gate currently verifies:

- presence of all three internal contracts;
- contract ownership;
- internal endpoint placement in owning modules;
- absence of Payment/Customer/Reporting API ownership inside `integration`;
- presence of the expected Phase 6 capabilities as implementation progresses;
- final OpenAPI-to-Spring endpoint/security/error conformance.

**Target lots:** progressively completed from 6.1 to 6.6.

The baseline gate introduced with Lot 6.0 intentionally checks only durable architectural invariants. It must not hard-code temporary implementation gaps that should disappear in subsequent lots.

## 8. Phase 6 backlog from baseline

| ID | Gap | Severity | Target lot |
|---|---|---|---|
| P6-001 | Payment `observedCustomerId` filter returns an empty page | High | 6.1 |
| P6-002 | ObservedCustomer exposes non-contractual `snapshotAt` request parameter | Medium | 6.2 |
| P6-003 | Reporting Payment Audit capability absent | Critical | 6.3–6.5 |
| P6-004 | Internal contracts remain `PENDING_APPROVAL` | Governance | 6.6 |
| P6-005 | No complete automated OpenAPI/implementation conformance gate | High | 6.1–6.6 |

## 9. Lot 6.0 exit criteria

Lot 6.0 is complete when:

- the three authoritative internal contracts are identified;
- each contracted operation is mapped to its current implementation chain;
- module ownership is documented;
- known implementation and contract drifts are recorded;
- the Phase 6 backlog is explicitly derived from those gaps;
- a durable baseline architecture test protects the source-of-truth and module ownership rules;
- no production-readiness claim is introduced.

## 10. Decision

```text
LOT_6_0_BASELINE = COMPLETE

PAYMENT_QUERY = PARTIAL_CONFORMANCE
OBSERVED_CUSTOMER_QUERY = PARTIAL_CONFORMANCE
PAYMENT_AUDIT_QUERY = NOT_IMPLEMENTED

NEXT_LOT = 6.1_PAYMENT_QUERY_COMPLETION
```
