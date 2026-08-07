# Kafka Outbox, DLQ and Replay Runbook

## Outbox

- Never mark delivered before broker acknowledgement.
- A failed publication remains eligible for a later relay attempt.
- Cleanup deletes only delivered records older than the configured retention.

## DLQ

DLQ metadata must retain event ID, event type, original topic, partition,
offset, consumer group, retry count, failure category and safe error code.

Do not place secrets, raw bank data or stack traces with personal information
in DLQ headers.

## Replay

Replay keeps the original event payload and original event ID. It adds a
separate replay ID and audit metadata. Generating a new event ID is forbidden,
because it bypasses consumer deduplication.

Replay requires an operator identity and a reason.
