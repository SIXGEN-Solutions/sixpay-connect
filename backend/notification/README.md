## Persistence ownership

Notification owns the following production table families:

| Table/family | Purpose |
|---|---|
| notification_deliveries | Partner notification delivery state |
| sixpay.operational_notification_deliveries | Operational notification state |
| sixpay.operational_notification_attempts | Operational delivery attempts |
| sixpay.operational_notification_replays | Operational replay requests |

The schema is maintained by:
backend/notification/src/main/resources/db/migration/V600__notification_baseline.sql
