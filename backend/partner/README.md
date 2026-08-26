## Persistence ownership

Partner owns the following production tables:

| Table | Purpose |
|---|---|
| partners | Partner aggregate |
| partner_authorized_perimeters | Partner access perimeters |
| partner_validation_thresholds | Current validation thresholds |
| partner_validation_threshold_history | Immutable threshold history |
| partner_audit | Immutable Partner audit records |
| partner_idempotency | Mutation idempotency records |
| partner_outbox_events | Partner integration events awaiting delivery |

The schema is maintained by:
backend/partner/src/main/resources/db/migration/V100__partner_baseline.sql
