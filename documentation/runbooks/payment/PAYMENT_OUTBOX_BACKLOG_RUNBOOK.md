# Payment Outbox Backlog Runbook

## Trigger

Use this runbook when any of the following increases:

```text
pendingOutbox
deadOutbox
staleProcessingOutbox
```

## Safety rules

- Do not delete outbox rows manually.
- Do not change an event ID.
- Do not publish the same event through an ad-hoc script.
- Do not mark an event `PUBLISHED` without transport evidence.
- Preserve Payment, audit and outbox atomicity.

## Investigation

1. Confirm PostgreSQL availability.
2. Group rows by status and event type.
3. Check the age of the oldest pending row.
4. Inspect claim owner and claim timestamp for stale processing rows.
5. Correlate failures through `correlation_id`.
6. Determine whether the transport publisher is deployed and healthy.
7. Confirm consumer idempotency before any replay.

## Recovery

The current Payment module contains the outbox storage foundation but no Kafka
publisher in this phase. Recovery must follow the approved future outbox relay
runbook once that component exists.
