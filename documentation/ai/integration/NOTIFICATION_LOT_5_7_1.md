# Lot 5.7.1 Implementation Record

Implemented:

- two provider-neutral operational trigger records;
- logical event names for `payment.posted.v1` and
  `accounting.batch.completed.v1`;
- SIXPAY-admin recipient abstraction;
- EMAIL/SMS/WEBHOOK canonical channel enum without provider implementation;
- versioned template keys and classpath template resources;
- template-variable allow-list catalogue;
- canonical delivery lifecycle;
- deterministic SHA-256 functional deduplication key;
- operational routing baseline to SIXPAY admin email;
- receiving-side planning use case;
- recipient resolver and ID-generator ports;
- tests for routing, lifecycle, deduplication and module boundaries.

Explicitly not implemented:

- Payment/Accounting composition adapters;
- persistence of notification intents;
- unique database constraint for deduplication;
- retry scheduler / DLQ;
- SMTP/provider delivery;
- SMS;
- generic webhook;
- Kafka transport;
- TresorPay Callback API 3.

The TresorPay callback remains owned by Payment.
