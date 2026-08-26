# Customer Observation — Operations Runbook

## Health

Check the application health endpoint and inspect only aggregate components:

- projection health;
- query health;
- audit health;
- database connectivity.

Expected states:

- `UP`: capability operating normally;
- `DEGRADED`: delayed projection, elevated bounded failure rate or audit write failures;
- `DOWN`: repository unavailable, excessive projection lag or terminal operational condition.

Health details must not contain IDs, correlation IDs, SQL messages or event
payloads.

## Projection backlog

Investigate:

1. Outbox claimable count;
2. oldest claimable event age;
3. PROCESSING rows older than processing timeout;
4. retry-exhausted and dead-letter counters;
5. Customer database health;
6. recent deployment or migration changes.

Do not log or export complete Outbox payloads.

## Query degradation

Inspect:

1. database reachability;
2. query failure-rate metric;
3. oldest projection age;
4. connection-pool saturation;
5. authorization failures by bounded result category.

Never add NIU, legal name, cursor or customer identifiers as metric tags.

## Audit degradation

Projection audit follows fail-closed behavior. A mutation must roll back if its
required audit cannot be appended.

Query audit follows fail-open behavior. The business read may complete, while
the audit failure is recorded using a bounded failure metric and safe log.

## Recovery

- Correct the infrastructure condition.
- Confirm database and migrations.
- Verify stale claims become claimable after timeout.
- Resume bounded dispatch.
- Confirm replay returns `REPLAYED` or stale returns `IGNORED_STALE`.
- Verify no duplicate observed payment or processed source event is created.

## Escalation evidence

Collect only:

- timestamp;
- environment;
- release/commit;
- bounded result/error category;
- aggregate counts;
- duration and lag;
- sanitized stack trace retained in secured technical logs.

Do not include customer identity, NIU, account reference, fingerprint, JWT,
API key or event payload.
