# Partner Module

## Purpose

Partner is the reference business module for SIXPAY CONNECT. It demonstrates
the repository conventions for domain modeling, application ports, secure HTTP
APIs, persistence, Outbox events and layered tests.

## Responsibilities

- create, validate, reject, suspend and reactivate partners;
- configure validation thresholds;
- expose partner catalog, status and audit queries;
- persist immutable audit and threshold history;
- publish versioned Partner integration events through its Outbox.

## API

Base path: /api/v1/partners

The API supports partner creation, listing, retrieval, validation, suspension,
reactivation, threshold configuration, status and audit queries. Mutating
requests use correlation and idempotency controls.

## Structure

    api/
    application/
    domain/
    infrastructure/
    configuration/
    events/

The module is non-executable. Bootstrap assembles it through its auto-
configuration; Partner itself does not depend directly on Notification,
Payment or the transport implementation.

## Boundaries

- Security provides the authenticated principal and authorities.
- Partner owns Partner business rules, persistence and event production.
- Integration relays Outbox messages using the configured transport.
- Consumers receive versioned events and do not call Partner to reconstruct
  the event decision.

## Persistence and reliability

Partner owns its Flyway migration, audit tables, threshold history, Outbox
records and idempotency store. Invalid transitions are rejected in the domain.
Audit and history are immutable, and concurrent mutations are protected by
database constraints and transactional idempotency.

## Validation

From backend:

    mvn -pl partner -am clean verify
    mvn -pl partner -am -Pfull-tests clean verify
    mvn -pl partner -am -Pcoverage clean verify

The full-tests command requires Docker for PostgreSQL integration tests.

## Persistence ownership

Partner owns these production tables:

| Table | Purpose |
|---|---|
| partners | Partner aggregate |
| partner_authorized_perimeters | Partner access perimeters |
| partner_validation_thresholds | Current validation thresholds |
| partner_validation_threshold_history | Immutable threshold history |
| partner_audit | Partner audit records |
| partner_idempotency | Mutation idempotency records |
| partner_outbox_events | Partner integration events |

Schema:
backend/partner/src/main/resources/db/migration/V100__partner_baseline.sql
