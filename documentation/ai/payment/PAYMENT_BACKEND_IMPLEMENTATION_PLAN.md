# SIXPAY CONNECT — Phase 3 Payment Backend Implementation Plan

## Status

| Field | Value |
| --- | --- |
| Phase | 3 — Payment Backend Implementation |
| Current lot | 3.1 — Backend Foundation |
| Authoritative branch | `feat/payment-domain-generation-brief` |
| Payment Domain Kernel | **FROZEN** |
| Global backend generation | **FORBIDDEN** |
| Lot-scoped generation | **AUTHORIZED FOR LOT 3.1 ONLY** |

## Purpose

Phase 3 builds the backend vertical layers around the existing Payment Domain
Kernel. It does not recreate, simplify, rename or relocate the aggregate,
immutable state, policies, policy profiles, Domain Services, invariants,
transitions or Domain Events.

## Frozen baseline

```text
1 Aggregate Root
1 immutable PaymentState
17 named operations
17 states
38 legal transitions
76 invariant and transition checks
33 explicit Domain Events
14 Policies
12 Policy Profiles
4 Domain Services
```

## Implementation sequence

| Lot | Name | Primary outcome |
| --- | --- | --- |
| 3.1 | Backend Foundation | Dependencies, canonical boundaries and architecture gates |
| 3.2 | Persistence Foundation | Domain-neutral JPA model, repository adapter and migration design |
| 3.3 | Audit Foundation | Immutable Payment audit trail |
| 3.4 | Outbox Foundation | Transactional event persistence without direct broker publication |
| 3.5 | Idempotency Foundation | Replay, conflict detection and concurrency protection |
| 3.6 | Application Layer | Commands, queries, views and ports |
| 3.7 | Payment Orchestration | Cohesive application services by workflow |
| 3.8 | Banking Adapters | Approved verification, funds, posting, lookup and reversal adapters |
| 3.9 | TFJ and Reconciliation | End-of-day confirmation and authoritative reconciliation |
| 3.10 | REST and Query APIs | Contract-derived controllers, DTOs and error handling |
| 3.11 | Security | Authentication, authorization and object-level partner isolation |
| 3.12 | Observability | Metrics, tracing, safe logs, health and runbooks |
| 3.13 | End-to-End Integration | TresorPay-to-final-result workflow validation |
| 3.14 | Performance and Concurrency | Load, locking, replay and virtual-thread validation |
| 3.15 | Final Validation | Maven verify, CI, contracts, migrations and synchronized documentation |

## Lot 3.1 authorized changes

Lot 3.1 may only:

- update `backend/payment/pom.xml` with dependencies already governed by the
  SIXPAY BOM and proven by the Golden Partner module;
- create documented canonical boundaries under `api`, `application`,
  `configuration`, `events` and `infrastructure`;
- strengthen architecture tests without changing Payment behavior;
- add the Phase 3 plan, manifest and source baseline.

Lot 3.1 shall not:

- modify any existing source under `com.sixpay.payment.domain`;
- create controllers, services, ports, repositories, JPA entities, migrations,
  adapters, schedulers, listeners or broker publishers;
- modify OpenAPI, integration-event or database contracts;
- activate Amplitude or other bank-specific configuration;
- fabricate Product, Architecture or Engineering approval.

## Lot 3.1 exit criteria

```text
Payment Domain Kernel unchanged
Canonical package boundaries present and documented
Payment dependencies aligned with Golden Partner conventions
Architecture tests protect inward-only dependencies
PaymentModule remains a non-executable marker
No controller, service, adapter, repository, entity or migration generated
mvn --batch-mode --no-transfer-progress -pl payment -am test succeeds
```

The Maven result may only be marked successful after the command has actually
been executed and its exit code observed.

## Financial safety rules for subsequent lots

- Persist the Payment intent before any external financial call.
- Treat timeout as an unknown outcome, never as proof of failure.
- Never automatically repeat a posting or reversal instruction.
- Resolve uncertain financial outcomes through authoritative lookup.
- Persist Payment state, audit, outbox and idempotency result atomically.
- Never publish directly to Kafka from the aggregate or inside the business
  transaction.
- Keep the domain model separate from persistence entities and HTTP DTOs.
- Never allow Notification to mutate Payment financial state.

## Source-of-truth rule

When this plan conflicts with implementation, architecture, requirements or an
approved contract, the precedence defined by `ENGINEERING_CONTEXT.md` applies.
