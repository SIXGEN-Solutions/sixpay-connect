# SIXPAY CONNECT — Payment Test Plan and Traceability

> **Lot:** `8 — Plan de tests et traçabilité`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `NORMATIVE_IMPLEMENTED`

## 1. Scope

Lot 8 does not duplicate the existing domain tests. It provides a complete,
machine-verifiable link:

```text
source requirement
→ invariant
→ transition
→ Aggregate Root operation
→ Domain Event
→ named test scenario
→ current or future test file
```

## 2. Coverage counts

```text
Invariants with named scenario : 76
Transitions with named scenario: 38
Domain Events referenced       : 33
Aggregate operations           : 17
Future vertical scenarios      : 15
```

## 3. Domain tests

| Area | Status | Evidence |
| --- | --- | --- |
| Value Objects and invalid references | Current domain unit tests | PaymentIdentityValueObjectsTest, ProtectedAccountValueObjectsTest, TreasuryAllocationIntentTest |
| Zero/negative amount and currency/allocation rules | Current domain unit/property scenarios | TreasuryAllocationIntentTest and Value Object tests |
| All legal transitions | Named scenarios PAY-TEST-TR-001..038 | Lifecycle suites plus traceability validation |
| All forbidden transitions | Exhaustive state/command complement | PaymentDomainKernelCatalogueTest and terminal-state tests |
| Atomicity after failure/conflict | Current lifecycle tests | State, version and event count remain unchanged |
| Terminal states | Current domain tests | PaymentTerminalStateProtectionTest |
| Unknown posting/reversal outcomes | Current lifecycle tests | Authoritative lookup only; no blind retry |
| Event confidentiality | Current architecture/event tests | Record-component inspection and denylist |

## 4. Future application tests

The future `application` layer must implement:

- intake and durable persistence before any external request;
- identical replay and original-result restitution;
- idempotency and external-reference conflict;
- valid/invalid authorization evidence;
- banking verification and funds control orchestration;
- posting and authoritative lookup after timeout;
- atomic Payment/audit/Outbox transaction;
- Notification independence from financial state;
- TFJ integrated, missing, ambiguous and conflicting evidence.

These scenarios are named under `PAY-TEST-APP-*` in
`PAYMENT_TEST_TRACEABILITY.yaml`.

## 5. Future persistence tests

The future `infrastructure.persistence` layer must verify:

- uniqueness of external and public references;
- optimistic locking on `businessVersion`;
- complete transaction rollback;
- audit and Outbox rollback with Payment;
- approved reference and period queries;
- protected account storage and masking;
- deterministic concurrent request and callback handling.

These scenarios are named under `PAY-TEST-PERS-*`.

## 6. Architecture tests

Permanent architecture guarantees:

```text
domain has no Spring/JPA/Kafka/HTTP
domain has no direct dependency on another business module
domain has no controller, external client or repository implementation
Domain Events expose no Aggregate Root or evidence snapshot
future Integration Events expose no JPA entity or Aggregate Root
```

## 7. Implementation status semantics

```text
IMPLEMENTED_OR_CURRENT_DOMAIN_TEST
→ current domain/architecture suite contains the automated proof category

IMPLEMENTED_CYCLE_SUITE_NAMED_SCENARIO_REQUIRED
→ lifecycle suite exists; Lot 8 gives every transition a stable scenario name

FUTURE_VERTICAL_TEST
→ application/persistence layer is not authorized yet; target test file is fixed

PLANNED_TEST
→ scenario is mandatory but awaits its owning vertical increment
```

A future test is never represented as already passing.

## 8. Exit criterion

The machine-readable validation fails when:

- one of the 76 invariants has no named scenario;
- one of the 38 transitions has no named scenario;
- a transition lacks an Aggregate operation or Domain Event;
- a scenario lacks a current/future test file;
- an identifier referenced by the matrix does not exist in its normative catalogue.
