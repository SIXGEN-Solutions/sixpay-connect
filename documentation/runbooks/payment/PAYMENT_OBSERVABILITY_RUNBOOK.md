# Payment Observability Runbook

## Purpose

Diagnose Payment failures without inspecting or exposing sensitive financial
payloads.

## Primary signals

### Metrics

```text
sixpay.payment.operations
sixpay.payment.operation.duration
```

Required tags:

```text
operation
outcome
```

Forbidden tags include Payment IDs, account references, customer IDs, external
references and Partner subjects.

### Tracing

Observation name:

```text
sixpay.payment.operation
```

Low-cardinality keys:

```text
payment.operation
payment.outcome
```

When a Micrometer tracing bridge is configured by the bootstrap application,
these observations create spans automatically.

### Logs

Structured lifecycle messages:

```text
payment_operation_started
payment_operation_completed
```

Required MDC field:

```text
correlationId
```

Never log account data, request/response payloads, OAuth tokens, complete
evidence snapshots or idempotency response bodies.

### Health

Component:

```text
payment
```

Details:

```text
database
pendingOutbox
deadOutbox
staleProcessingOutbox
staleIdempotency
```

Database failure marks the component DOWN. Backlog counts are diagnostic
details and require operational threshold alerts.

## First response

1. Capture the correlation ID.
2. Inspect `sixpay.payment.operations` grouped by operation and outcome.
3. Inspect duration percentiles for the failing operation.
4. Follow the trace named `sixpay.payment.operation`.
5. Check Payment health details.
6. Identify whether the issue belongs to persistence, idempotency, outbox,
   Core Banking, TFJ reconciliation or query projection.
7. Do not replay a financial command solely because a timeout occurred.

## Escalation

Escalate immediately when:

- database health is DOWN;
- dead outbox rows are present;
- stale `PROCESSING` outbox rows increase continuously;
- stale idempotency rows block new requests;
- posting outcome is unknown;
- TFJ evidence conflicts with a confirmed posting;
- reversal is required but not authorized.
