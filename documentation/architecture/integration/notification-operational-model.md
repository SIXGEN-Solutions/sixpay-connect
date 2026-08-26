# Lot 5.7.1 — Operational Notification model and triggers

## 1. Scope

Lot 5.7.1 introduces only the provider-neutral Notification model:

- `payment.posted.v1` trigger;
- `accounting.batch.completed.v1` logical trigger;
- SIXPAY administrator recipients;
- versioned template catalogue;
- delivery lifecycle;
- functional deduplication.

No SMTP call, Kafka listener/publisher, webhook delivery or persistence is
introduced in this lot.

## 2. Module boundary

Notification owns consumption semantics and delivery-channel abstractions.

Payment and Accounting own the business facts that trigger notifications.

Notification therefore contains no Java dependency on either the `payment` or
`accounting` module. While the modules are co-deployed, composition adapters may
map producer facts into the receiving-side Notification trigger records and
invoke the use case in-process.

Kafka remains optional and must not become the default communication mechanism
for the first modular-monolith delivery.

## 3. Triggers

### 3.1 Payment posted

Logical source event:

`payment.posted.v1`

The Payment repository already defines this distributed event name. For the
modular monolith, the same semantic fact may be mapped in-process without Kafka.

Notification receives only:

- Payment ID;
- public Payment reference;
- partner ID;
- amount;
- currency;
- posting timestamp;
- correlation ID.

Raw bank-account data, NIU, credentials and provider payloads are not part of
the Notification trigger.

### 3.2 Accounting batch completed

Logical source event reserved by this lot:

`accounting.batch.completed.v1`

Notification receives only:

- Accounting batch ID;
- business date;
- financial-institution code;
- item count;
- completion timestamp;
- correlation ID.

The trigger is emitted only for the Accounting business state `COMPLETED`, not
for transport states such as `SUBMITTED` or `OUTCOME_UNKNOWN`.

## 4. TresorPay callback exclusion

TresorPay Callback API 3 remains owned by Payment.

Although the callback functionally informs TresorPay of Payment progress, it is
a partner integration with its own outbox, security and JWS rules. It is not a
Notification delivery channel and must not be moved into this module.

## 5. Recipient model

Lot 5.7.1 supports the logical recipient type:

`SIXPAY_ADMIN`

The model stores an opaque `reference`, for example an administrator/directory
identifier. It intentionally does not require an email address.

A later adapter resolves this reference or the configured administrator group
to an actual email destination.

Default locale is French when none is specified.

## 6. Routing baseline

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| `PAYMENT_POSTED` | SIXPAY admin | EMAIL | `PAYMENT_POSTED_ADMIN_V1` |
| `ACCOUNTING_BATCH_COMPLETED` | SIXPAY admin | EMAIL | `ACCOUNTING_BATCH_COMPLETED_ADMIN_V1` |

SMS and generic webhook are canonical future channels only. No transport is
implemented for them in this lot.

## 7. Template model

Templates are identified by stable, versioned keys and classpath resources.

### PAYMENT_POSTED_ADMIN_V1

Allowed variables:

- `paymentId`
- `paymentReference`
- `partnerId`
- `amount`
- `currency`
- `postedAt`

### ACCOUNTING_BATCH_COMPLETED_ADMIN_V1

Allowed variables:

- `batchId`
- `businessDate`
- `financialInstitutionCode`
- `itemCount`
- `completedAt`

`OperationalNotificationTemplateCatalog` is the allow-list. Planning fails if
a mapper attempts to provide a variable outside the approved list.

Rendered bodies are not persisted by this lot.

## 8. Delivery lifecycle

Canonical lifecycle:

`PENDING -> DISPATCHING -> ACCEPTED -> DELIVERED`

Retry path:

`DISPATCHING/ACCEPTED -> FAILED_RETRYABLE -> DISPATCHING`

Exhausted retry:

`FAILED_RETRYABLE -> DEAD_LETTERED`

Permanent failure:

`DISPATCHING/ACCEPTED -> FAILED_PERMANENT`

`DELIVERED`, `FAILED_PERMANENT` and `DEAD_LETTERED` are terminal.

`ACCEPTED` intentionally does not mean `DELIVERED`. A later channel adapter must
not claim end delivery unless the provider can prove it.

## 9. Deduplication

The functional deduplication key is SHA-256 over:

`triggerType | sourceId | recipientType | recipientReference | channel | templateKey`

Consequences:

- replaying the same trigger for the same administrator and template produces
  the same deduplication key;
- different administrators receive independent notifications;
- changing to a new template version creates a new functional notification;
- transport retry does not create another functional notification.

Persistence and the database uniqueness constraint for this key belong to Lot
5.7.2.
