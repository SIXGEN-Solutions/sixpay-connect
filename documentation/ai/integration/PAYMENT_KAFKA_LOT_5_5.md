# Lot 5.5 Implementation Record

Implemented:

- provider-neutral distributed-event envelope;
- strict separation from internal Java events;
- Kafka transport behind `DistributedEventTransport`;
- Kafka disabled by default;
- Payment event catalogue and JSON Schemas;
- topic routing and `paymentId` partition strategy;
- outbox relay contracts and cleanup;
- consumer idempotency by consumer name and event ID;
- retry/DLQ metadata model;
- audited replay preserving the event ID;
- publication, consumption, duplicate, error and lag metrics;
- crash-oriented unit tests.

Repository-specific persistence adapters for the existing Payment outbox and
consumer deduplication tables must use the existing persistence conventions.
The shared module intentionally contains only provider-neutral contracts and
mechanisms.
