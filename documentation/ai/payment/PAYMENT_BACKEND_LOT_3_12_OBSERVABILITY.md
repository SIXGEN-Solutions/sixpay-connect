# SIXPAY CONNECT — Phase 3 / Lot 3.12

## Observability

### Metrics

The module publishes low-cardinality counters and timers for focused Payment
application-service operations.

### Tracing

Micrometer Observation instruments each focused Payment service. A tracing
bridge configured by the bootstrap application converts observations to spans.

No tracing exporter or vendor-specific dependency is introduced in the
Payment module.

### Logging

Payment HTTP requests receive a UUID correlation ID in MDC and in the response
header. Lifecycle logs contain operation names, outcomes and exception types
only.

### Health

The Spring Boot 4 `HealthIndicator` checks PostgreSQL and reports durable-work
backlogs for outbox and idempotency.

### Runbooks

Four runbooks cover:

- general observability diagnosis;
- outbox backlog;
- unknown financial posting outcome;
- TFJ reconciliation.

### Repository alignment

The Payment module already depends on Spring Boot Actuator, so no Maven
dependency is added. The implementation uses the Spring Boot 4 health package:

```text
org.springframework.boot.health.contributor
```
