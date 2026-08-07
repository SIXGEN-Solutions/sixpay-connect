# Lot 5.5 — Events, Outbox and Kafka

## Delivery stance

Kafka is an optional transport. The initial modular-monolith delivery keeps
co-deployed interactions in-process.

```yaml
sixpay:
  integration:
    kafka:
      enabled: false
```

## Event flow

```text
Payment transaction
  -> internal Java domain event
  -> mapping to distributed event contract
  -> transactional outbox
  -> relay
      -> in-process transport (default modular monolith)
      -> Kafka transport (optional)
```

Internal domain events are never serialized directly.

## Published event catalogue

- `payment.received.v1`
- `payment.posted.v1`
- `payment.reversed.v1`
- `payment.failed.v1`
- `payment.reconciliation-required.v1`

## Topic families

- `sixpay.payment.lifecycle.v1`
- `sixpay.payment.financial.v1`
- `sixpay.payment.reconciliation.v1`

Partition key: `paymentId`.

## Delivery guarantee

The outbox record is marked delivered only after Kafka acknowledges the
producer send. If the process crashes before the mark, the record is published
again after restart. Consumers therefore use `(consumerName, eventId)` as the
functional deduplication key.

This provides at-least-once transport and effectively-once business handling.

## PII

Events may contain technical identifiers, public references, masked values,
fingerprints, amount, currency and status. Raw accounts, NIU, contact details,
tokens, secrets and raw provider payloads are forbidden.
