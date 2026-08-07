# Internal Operational Notification Trigger Contract v1

## Status

Receiving-side Java contract for the modular-monolith deployment.

This document does not mandate Kafka.

## Payment posted

Source semantic event: `payment.posted.v1`

Canonical fields:

| Field | Type | Required |
|---|---|---|
| paymentId | UUID | yes |
| publicPaymentReference | String | yes |
| partnerId | String | yes |
| amount | decimal > 0 | yes |
| currency | ISO-4217 currency | yes |
| postedAt | Instant | yes |
| correlationId | String | yes |

## Accounting batch completed

Source semantic event: `accounting.batch.completed.v1`

Canonical fields:

| Field | Type | Required |
|---|---|---|
| batchId | UUID | yes |
| businessDate | LocalDate | yes |
| financialInstitutionCode | String | yes |
| itemCount | integer > 0 | yes |
| completedAt | Instant | yes |
| correlationId | String | yes |

## Privacy baseline

The trigger contracts exclude:

- raw account number;
- NIU;
- full Customer identity;
- email destination;
- telephone number;
- authentication token;
- API key;
- private/signing keys;
- raw provider request/response payload.

## Ownership

- Payment owns the `payment.posted` source fact.
- Accounting owns the `accounting.batch.completed` source fact.
- Notification owns trigger-consumption semantics, routing and templates.
- Bootstrap/composition owns in-process mapping while the modules are
  co-deployed.
