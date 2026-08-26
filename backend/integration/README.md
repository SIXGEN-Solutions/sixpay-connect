# Integration Module

## Purpose

The Integration module provides provider-neutral transport, messaging, Outbox
relay, correlation, resilience, Kafka and consumer-idempotency capabilities.
Business rules and provider-specific mappings remain in their owning modules.

## Transport

The active transport is selected by configuration:

| Value | Use |
|---|---|
| internal | In-process communication in the modular monolith |
| kafka | Distributed transport for a future service deployment |

The transport changes only the implementation of IntegrationEventTransport. It
does not change domain events or module-owned Outbox records.

## Reference configuration

    sixpay.messaging.transport: internal
    sixpay.messaging.outbox.enabled: true
    sixpay.messaging.outbox.polling-delay: 1000
    sixpay.messaging.outbox.batch-size: 50
    sixpay.messaging.outbox.max-attempts: 5
    sixpay.messaging.outbox.retry-delay: 30s
    sixpay.messaging.outbox.processing-timeout: 5m

Kafka-specific properties remain under spring.kafka and the sixpay.messaging.kafka
namespace.

## Guarantees

- aggregate state and the module Outbox record share one transaction;
- claims use PostgreSQL row locking with SKIP LOCKED;
- retryable failures are recorded with a next attempt time;
- exhausted deliveries move to DEAD;
- abandoned PROCESSING claims become eligible again after the timeout;
- delivery is at least once, so consumers must be idempotent.

## Boundaries

Integration is not an omnipotent domain service. It does not own Partner,
Customer, Payment, Accounting or Notification business decisions. Provider
payloads and mappings stay in the owning domain.

## Validation

From backend:

    mvn -pl integration -am test
    mvn -pl integration -am clean verify
    mvn -pl integration -am -Pfull-tests clean verify

The full-tests command requires Docker for PostgreSQL-backed integration tests.
