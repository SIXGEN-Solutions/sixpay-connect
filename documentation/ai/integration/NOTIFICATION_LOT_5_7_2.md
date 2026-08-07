# Lot 5.7.2 Implementation Record

Implemented:

- durable operational Notification delivery aggregate;
- immutable attempt history model;
- receiving-side orchestration that does not propagate failure to source
  Payment/Accounting calls;
- PostgreSQL `UNIQUE(deduplication_key)`;
- PostgreSQL `INSERT ... ON CONFLICT DO NOTHING`;
- atomic due-notification claim;
- optimistic versioning;
- bounded exponential retry;
- provider-neutral database DLQ (`DEAD_LETTERED`);
- attempt-level safe error codes;
- provider-neutral delivery gateway;
- retryable/permanent delivery exception boundary;
- retry processor that isolates each notification;
- optional scheduled retry runner activated only when a delivery gateway exists;
- dedicated persistence tables isolated from the legacy Partner notification
  implementation;
- tests and runbook.

Not implemented:

- actual email/SMTP provider;
- actual admin recipient resolution;
- SMS;
- webhook;
- Kafka dependency for the operational flow;
- provider delivery-status callback;
- stale DISPATCHING recovery/replay;
- purge/retention.
