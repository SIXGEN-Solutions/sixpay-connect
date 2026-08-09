# SIXPAY CONNECT — Subscription Golden Test Coverage

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.2 — Subscription
```

## 1. Reference

The `partner` module remains the golden business-module reference.

Subscription is evaluated against the same four coverage dimensions:

```text
Domain
Application
API
Infrastructure
```

However, the golden model SHALL only be applied to behavior that is actually
implemented.

Tests SHALL NOT be created for placeholder packages or planned capabilities.

---

## 2. Current implementation state

On the authoritative branch:

```text
feat/sixpay-test-validate-pilote
```

the Subscription module is currently a structural placeholder.

The module POM identifies:

```text
artifactId: subscription
packaging: jar
description: Subscription domain module for SIXPAY CONNECT
```

but contains no Subscription-specific dependencies.

The source tree currently retains placeholder `.gitkeep` files in:

```text
backend/subscription/src/main/java/com/sixpay/subscription/
├── api/
├── application/
├── domain/
└── infrastructure/
```

and the test tree currently retains:

```text
backend/subscription/src/test/java/.gitkeep
```

No implemented Subscription aggregate, application service, inbound API,
persistence adapter or module-local test responsibility is present in the
authoritative implementation branch.

---

## 3. Golden coverage assessment

| Dimension | Status | Reason |
|---|---|---|
| Domain | N/A | No Subscription domain implementation exists yet |
| Application | N/A | No Subscription application service/use case exists yet |
| API | N/A | No Subscription HTTP API exists yet |
| Infrastructure | N/A | No Subscription persistence or technical adapter exists yet |

Overall status:

```text
DEFERRED — NO IMPLEMENTED CAPABILITY TO TEST
```

This status does not mean that Subscription testing is optional.

It means that Phase 8.2 can only validate the implementation currently present
on the authoritative branch.

---

## 4. Why tests must not be generated now

Creating tests such as:

```text
SubscriptionTest
SubscriptionApplicationServiceTest
SubscriptionControllerTest
SubscriptionPersistenceIT
```

without corresponding production code would violate repository rules.

It would:

- invent behavior not present in the authoritative implementation;
- predefine APIs and transitions outside the implementation phase;
- risk diverging from published requirements and contracts;
- create test-driven architecture that may conflict with the eventual
  Subscription domain design;
- violate the repository rule that existing implementation must be inspected
  before proposing changes.

The golden `partner` module is a structural and quality reference, not a
template for generating fictional business behavior.

---

## 5. Future Subscription golden baseline

When Subscription implementation begins, the module SHALL be assessed using the
same pattern as `partner`.

Expected categories, only when implemented:

### Domain

```text
subscription lifecycle
invariants
value objects
legal transitions
illegal transitions
terminal states
```

### Application

```text
creation/request
approval/rejection
activation
suspension/reactivation
duplicate/replay handling
dependency failures
edge cases
```

### API

```text
HTTP status
request/response payload
Bean Validation
RBAC
scopes
error mapping
correlation
```

### Infrastructure

```text
mapping
persistence
uniqueness
ordering
pagination if applicable
optimistic locking/conflicts
database constraints
```

The actual names, states, operations and endpoints SHALL be taken from the
authoritative Subscription implementation and contracts at that time.

They SHALL NOT be invented by Phase 8.2.

---

## 6. Future test structure

Once production code exists, tests SHOULD follow the golden structure:

```text
backend/subscription/
└── src/test/java/com/sixpay/subscription/
    ├── api/
    ├── application/
    │   └── service/
    ├── domain/
    └── infrastructure/
        └── persistence/
```

Only directories containing real tests SHALL be created.

---

## 7. Cross-module boundary

Phase 8.2 remains module-local.

Future Subscription scenarios requiring:

```text
customer
partner
payment
accounting
```

or the assembled application belong to Phase 8.3 cross-module integration
testing, not to 8.2.2.

---

## 8. 8.2.2 exit criteria

8.2.2 is complete when:

- the Subscription implementation state is explicitly recorded;
- no fictional test behavior has been generated;
- all four golden dimensions are classified as `N/A` for the current branch;
- the module is marked `DEFERRED` until implementation exists;
- the golden `partner` structure remains the future reference;
- no production code, POM or placeholder package has been modified.

Final status:

```text
8.2.2 SUBSCRIPTION — COMPLETE FOR CURRENT IMPLEMENTATION STATE
```
