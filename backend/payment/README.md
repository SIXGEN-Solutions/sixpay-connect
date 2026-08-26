## Persistence ownership

Payment owns the following production tables:

| Table | Purpose |
|---|---|
| payments | Payment aggregate and lifecycle state |
| payment_audit | Immutable Payment audit records |
| payment_outbox_events | Payment integration events awaiting delivery |
| payment_idempotency | Payment command idempotency and replay data |
| payment_observed_customer_link | Payment-side link to an ObservedCustomer projection |

Payment does not own Customer, CustomerSubscription, Accounting or Reporting
tables. Cross-module access uses application ports and published contracts.

The schema is maintained by:
backend/payment/src/main/resources/db/migration/V300__payment_baseline.sql
