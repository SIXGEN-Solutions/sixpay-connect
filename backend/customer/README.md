## Persistence ownership

Customer owns the following production table families:

| Table/family | Purpose |
|---|---|
| customer_management_customer | Local customer enrollment and lifecycle |
| customer_management_bank_account | Customer bank-account references and default-account state |
| customer_management_subscription | Local CustomerSubscription lifecycle |
| customer_management_audit | Audit of customer-management actions |
| customer_observed_customer | ObservedCustomer read projection |
| customer_observed_institution | Observed banking institution projection |
| customer_observed_account | Observed account projection |
| customer_observed_payment | Observed payment projection |
| customer_observation_processed_event | Observation ingestion idempotency |
| customer_observation_audit | Observation audit records |
| customer_observed_master_link | Link between observed and local customer records |

The external TRESOR PAY subscription is not stored as a CustomerSubscription
record. Its ownership remains external and outside the Payment MVP.

The schema is maintained by:
backend/customer/src/main/resources/db/migration/V200__customer_baseline.sql
