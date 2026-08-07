# Lot 5.7.2 — Operational Notification persistence and orchestration

## Objective

Persist Notification intents produced by Lot 5.7.1, deduplicate functional
notifications, isolate provider failures, record every delivery attempt, apply
bounded retry and move exhausted notifications to a logical DLQ.

The source Payment/Accounting transaction must never depend on successful
notification delivery.

## Boundary with the legacy Notification implementation

The repository already contains a historical Partner-status notification
pipeline with:

- `NotificationDeliveryJpaEntity`;
- persistence statuses `PENDING/PROCESSING/SENT/FAILED/DEAD`;
- Partner-specific sender/retry services.

Lot 5.7.2 does not rewrite that legacy flow.

The operational capability uses dedicated tables and packages:

- `infrastructure.operational.persistence`;
- `operational_notification_deliveries`;
- `operational_notification_attempts`.

This avoids changing existing Partner behavior while the new canonical
Notification lifecycle is introduced.

## Registration flow

```text
Payment / Accounting source fact
        |
        | composition/outbox/post-commit boundary
        v
OperationalNotificationOrchestrationService.accept()
        |
        v
5.7.1 planning
        |
        v
NotificationIntent(PENDING)
        |
        v
saveIfAbsent(deduplicationKey)
```

`accept()` is deliberately non-throwing. A persistence/planning failure returns
a failed registration result rather than propagating an exception into the
source business transaction.

For reliability, the composition adapter should still invoke this boundary from
a source outbox or post-commit delivery path. The non-throwing API protects the
source transaction; it does not turn a database outage into a successful
notification.

## Functional idempotence

Database invariant:

```text
UNIQUE(deduplication_key)
```

The key was defined in Lot 5.7.1 as SHA-256 of:

```text
triggerType
| sourceId
| recipientType
| recipientReference
| channel
| templateKey
```

Persistence uses PostgreSQL:

```text
INSERT ... ON CONFLICT (deduplication_key) DO NOTHING
```

Therefore concurrent consumers cannot create two functional notification rows.

## Delivery attempts

Each claimed delivery increments `attempt_count`.

An attempt record stores only:

- attempt ID;
- notification ID;
- attempt number;
- start timestamp;
- completion timestamp;
- canonical outcome;
- safe error code.

No exception stacktrace, SMTP payload, email body, access token or provider
response body is persisted.

`(notification_id, attempt_number)` is unique.

## Claiming and concurrency

Due rows are:

- `PENDING`, or
- `FAILED_RETRYABLE`;

with `next_attempt_at <= now`.

Claiming is one atomic SQL update:

```text
PENDING / FAILED_RETRYABLE
        ->
DISPATCHING
```

and increments the attempt counter.

A second worker attempting to claim the same row receives no claim and skips it.

## Delivery gateway

Lot 5.7.2 introduces only:

```text
OperationalNotificationDeliveryGateway
```

Provider implementations belong to 5.7.3+.

The gateway may return:

- `ACCEPTED`;
- `DELIVERED`;

or throw:

- `RetryableNotificationDeliveryException`;
- `PermanentNotificationDeliveryException`.

Unknown runtime failures are conservatively classified as retryable, but the
retry count remains bounded.

## Retry policy

Provisional defaults:

```text
maxAttempts    = 5
initialBackoff = 30 seconds
maxBackoff     = 15 minutes
batchSize      = 50
pollInterval   = 30 seconds
```

Backoff is exponential and capped:

```text
30s -> 60s -> 120s -> 240s -> ... capped at 15m
```

All values are configuration-driven.

## DLQ

DLQ is deliberately provider-neutral and database-backed in this modular
monolith phase:

```text
FAILED_RETRYABLE
        |
        | max attempts exhausted
        v
DEAD_LETTERED
```

No Kafka DLQ is required.

This is consistent with the rule that Kafka is not the default internal
communication mechanism while modules remain co-deployed.

## Source transaction isolation

Notification delivery is never performed from the trigger-registration call.

Registration creates a durable intent; retry workers dispatch it separately.

A provider failure therefore changes only Notification state:

```text
Payment POSTED remains POSTED
Accounting COMPLETED remains COMPLETED
Notification may become FAILED_RETRYABLE / FAILED_PERMANENT / DEAD_LETTERED
```

The source aggregate is never rolled back.

## Accepted versus delivered

`ACCEPTED` and `DELIVERED` remain distinct.

If a provider can prove immediate final delivery, the orchestration validates
the lifecycle logically as:

```text
DISPATCHING -> ACCEPTED -> DELIVERED
```

and persists the final `DELIVERED` state.

If a provider only acknowledges acceptance, state remains `ACCEPTED` until a
future delivery-status mechanism proves final delivery.

## Deferred

Lot 5.7.3:

- administrator recipient resolution;
- SMTP/email gateway;
- template rendering;
- provider error classification.

Lot 5.7.4:

- delivery-status callbacks/polling if the provider supports them;
- stale `DISPATCHING` crash recovery policy;
- operator replay of `DEAD_LETTERED`;
- retention/purge;
- dashboards and alerts.
