# SIXPAY CONNECT — Payment Backend Phase 3 Final Validation

## Validation status

```text
CONDITIONALLY_BLOCKED
```

The implementation has reached the final validation stage, but the repository
cannot yet be declared fully green until the contract alignment patch is
applied and the required external contract approvals are recorded.

## Source-of-truth findings

### Build

The Maven parent supports:

```text
clean verify
full-tests
coverage
Java 21
Maven Enforcer
Surefire
Failsafe
JaCoCo report generation
```

### CI

The existing Backend CI runs the whole reactor twice:

```text
clean verify
clean verify -Pfull-tests,coverage
```

The new Payment-specific workflow adds a focused path-filtered gate, requires
the Payment reports to exist and uploads them as release evidence.

### OpenAPI and contracts

The internal Payment Query API is read-only and correctly exposes:

```text
GET /internal/api/v1/payments
GET /internal/api/v1/payments/{paymentId}
```

Its `PaymentStatus` schema is stale compared with the authoritative Java
implementation. The included patch aligns the schema with the 17 implemented
statuses.

The contract metadata still declares:

```text
approvalStatus: PENDING_APPROVAL
generationPolicy: REFERENCE_ONLY
codeGenerationAllowed: false
```

This remains a governance blocker for production activation.

### Architecture

The final architecture gate protects:

- framework-free domain;
- no application-to-infrastructure dependency;
- read-only REST API;
- Posting separated from TFJ Reconciliation;
- no direct Payment dependency on Accounting or Notification;
- non-executable Payment module.

### Quality gates

Automated gates cover:

- compilation;
- unit tests;
- integration tests;
- architecture;
- OpenAPI lifecycle parity;
- PostgreSQL migrations;
- persistence/audit/outbox atomicity;
- idempotency/replay/concurrency;
- optimistic locking;
- virtual-thread and volume regressions;
- JaCoCo report generation.

No numeric coverage threshold is introduced because no approved threshold
exists in the repository.

## Exit criteria

Phase 3 may be marked complete only when:

1. the OpenAPI status patch is applied and reviewed;
2. `validate-payment-phase3` succeeds locally;
3. the GitHub Payment Final Validation workflow succeeds;
4. all reports are archived;
5. contract-owner and security approvals are recorded;
6. no waived architecture or test failure remains.
