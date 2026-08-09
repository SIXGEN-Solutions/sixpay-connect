# SIXPAY CONNECT — Backend Golden Test Coverage

## 1. Purpose

This document tracks Phase 8.2 backend test coverage against the `partner`
golden-module testing model.

It does not define a new testing architecture.

Testing conventions, ownership, naming and CI execution remain defined by the
Phase 8.1 test foundation.

The `partner` module remains the golden business-module reference.

---

## 2. Golden coverage model

A business capability is assessed independently across the following layers.

### Domain

Validate, where applicable:

- invariants;
- legal transitions;
- illegal transitions;
- value objects;
- domain policies;
- deterministic domain calculations;
- terminal-state protection.

### Application

Validate, where applicable:

- happy path;
- rejected operations;
- output-port interaction;
- dependency failures;
- edge cases;
- orchestration decisions;
- transaction-independent application logic.

### API

Validate only when the module exposes an HTTP API:

- HTTP status;
- request payload;
- response payload;
- Bean Validation;
- RBAC;
- OAuth scopes;
- error mapping;
- correlation behavior.

### Infrastructure

Validate, where applicable:

- mapping;
- persistence;
- stable ordering;
- pagination;
- optimistic locking;
- database constraints;
- external adapter mapping;
- technical failure classification.

---

## 3. Golden reference

The `partner` module provides the canonical baseline.

Primary evidence includes:

```text
PartnerTest
PartnerApplicationServiceTest
PartnerControllerTest
PartnerPersistenceIT