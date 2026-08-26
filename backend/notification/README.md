# Notification Module

## Purpose

The Notification module owns delivery of Partner decision notifications and
operational notifications.

## Responsibilities

Partner decision notifications:

- consume integration events through Notification-owned adapters;
- select the supported notification template;
- deliver the notification without calling Partner directly.

Operational notifications:

- model notification requests and delivery state;
- schedule, retry, replay and retain operational deliveries;
- persist delivery state and operational metrics;
- deliver through configured SMTP or other supported channels.

## Boundaries

- Notification does not decide Partner, Payment or Accounting business state.
- Integration provides transport and event delivery support.
- Notification owns templates, routing and delivery lifecycle.
- Delivery is at least once and must be handled idempotently.

## Validation

From backend:

    mvn -pl notification -am test
    mvn -pl notification -am clean verify
    mvn -pl notification -am -Pfull-tests clean verify

The full-tests command requires Docker when PostgreSQL integration tests are
selected.

## Persistence ownership

Notification owns these production table families:

| Table/family | Purpose |
|---|---|
| notification_deliveries | Partner notification delivery state |
| sixpay.operational_notification_deliveries | Operational notification state |
| sixpay.operational_notification_attempts | Delivery attempts |
| sixpay.operational_notification_replays | Replay requests |

Schema:
backend/notification/src/main/resources/db/migration/V600__notification_baseline.sql
