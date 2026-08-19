# SIXPAY CONNECT — Phase 8.3.4 Payment / Notification

## Status

```text
8.3.4 readiness gate                         IMPLEMENTED
payment.posted.v1 contract                   PUBLISHED
Notification payment-posted receiving model IMPLEMENTED
Payment -> Notification production wiring   NOT IMPLEMENTED
```

No `pom.xml` change is required. Reuse the existing `assembled-tests` profile.

Run:

```bash
cd backend
mvn --batch-mode --no-transfer-progress -pl tests -am -Pfull-tests,assembled-tests -Dit.test=PaymentNotificationReadinessIT verify
```
