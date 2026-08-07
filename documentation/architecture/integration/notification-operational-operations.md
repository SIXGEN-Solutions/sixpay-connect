# Lot 5.7.4 — Operational Notification monitoring and operations

## Objective

Close the first operational Notification slice with:

- safe status inspection;
- controlled replay;
- low-cardinality Micrometer metrics;
- terminal-record retention and purge;
- operator runbook.

No new provider transport is introduced.

## Status inspection

`OperationalNotificationOperationsUseCase` exposes:

- lookup by notification ID;
- bounded list by canonical status;
- controlled replay.

The status view intentionally exposes only logical identifiers and safe
operational metadata:

- notification ID;
- source type and source ID;
- logical recipient reference;
- channel/template;
- canonical status;
- total attempt count;
- current replay-cycle attempt count;
- replay count;
- timestamps;
- safe error code;
- provider reference;
- correlation ID;
- attempt history.

It does not expose the resolved administrator email address or SMTP secrets.

## Controlled replay

Only `DEAD_LETTERED` may be replayed automatically by this capability.

`FAILED_PERMANENT` remains terminal because replaying an unchanged permanent
failure is unsafe and usually useless. A future privileged remediation flow may
define a different policy if a contract requires it.

Replay preserves:

- `notificationId`;
- source reference;
- recipient logical reference;
- template version;
- deduplication key;
- total attempt history.

Replay resets only the retry-cycle budget:

```text
DEAD_LETTERED
      |
      | operator + reason + timestamp
      v
FAILED_RETRYABLE
cycleAttemptCount = 0
nextAttemptAt = now
replayCount++
```

The next worker claim therefore receives a fresh bounded retry budget without
losing total attempt history.

## Replay audit atomicity

Replay state change and replay audit are persisted in one transaction.

Audit fields:

- replay ID;
- notification ID;
- operator reference;
- reason;
- previous status;
- requested timestamp.

A replay cannot be committed without its operator audit.

## Metrics

Micrometer metrics use no high-cardinality identifiers.

Gauges:

- `sixpay.notification.operational.status{status=...}`;
- `sixpay.notification.operational.due`;
- `sixpay.notification.operational.oldest.due.age.seconds`.

Counters:

- `sixpay.notification.operational.replay.total`;
- `sixpay.notification.operational.purged.total`.

No notification ID, source ID, recipient reference or correlation ID is used as
a metric tag.

## Retention

Default provisional policy:

| Data | Retention |
|---|---:|
| DELIVERED notification | 90 days |
| FAILED_PERMANENT | 365 days |
| DEAD_LETTERED | 365 days |
| attempts | same as parent notification |
| replay audits | same as parent notification |

Purge deliberately excludes:

- `PENDING`;
- `DISPATCHING`;
- `ACCEPTED`;
- `FAILED_RETRYABLE`.

Attempts and replay audits are removed through `ON DELETE CASCADE` only when
their terminal parent notification is purged.

## Purge batching

Purge is bounded to avoid long-running deletes:

```text
default batch size = 500
default schedule   = once per 24 hours
```

Each execution performs at most one configured batch. Repeated scheduled runs
eventually drain old terminal history without holding an unbounded transaction.

## No administrator REST endpoint yet

This lot intentionally exposes an application operations port, not a public
REST endpoint.

The repository does not yet contain an approved operations-role/API security
contract for manual replay. Inventing an endpoint or RBAC role here would
violate the source-of-truth rule.

A future administration adapter can expose the use case after its authentication,
authorization and audit contract is approved.
