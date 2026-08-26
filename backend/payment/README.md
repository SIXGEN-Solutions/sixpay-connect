# Payment Module

## Purpose

The Payment module owns payment business behavior, state transitions,
idempotency, audit and Outbox boundaries.

## Responsibilities

- accept and validate payment commands;
- coordinate customer, account and banking verification results;
- enforce Payment invariants and legal state transitions;
- persist payment state, audit records and Outbox records atomically;
- expose Payment query and timeline capabilities;
- reconcile external outcomes without blind financial replay.

## API

The module exposes the Payment query endpoints under:

    /internal/api/v1/payments

Payment audit timeline and audit export endpoints are owned by Reporting and
are documented by the corresponding internal contracts.

## Boundaries

- Integration owns provider-neutral transport only.
- Customer owns customer verification and CustomerSubscription.
- Accounting owns accounting batches and reconciliation.
- Reporting owns immutable Payment audit queries and exports.
- Payment does not manage external TRESOR PAY subscriptions.

## Structure

The module follows the Partner reference layering:

- api;
- application;
- domain;
- infrastructure;
- configuration;
- events.

## Validation

From backend:

    mvn -pl payment -am test
    mvn -pl payment -am clean verify
    mvn -pl payment -am -Pfull-tests clean verify

The full-tests command requires Docker when PostgreSQL integration tests are
selected.

## Persistence ownership

Payment owns these production tables:

| Table | Purpose |
|---|---|
| payments | Payment aggregate and lifecycle |
| payment_audit | Immutable Payment audit |
| payment_outbox_events | Payment integration events |
| payment_idempotency | Command idempotency and replay data |
| payment_observed_customer_link | Link to an ObservedCustomer projection |

Payment does not own Customer, CustomerSubscription, Accounting or Reporting
tables.

Schema:
backend/payment/src/main/resources/db/migration/V300__payment_baseline.sql
