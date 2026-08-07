# Lot 5.7.3 — SIXPAY administrator email channel

## Objective

Provide the first concrete delivery adapter for operational notifications:

- recipient: SIXPAY administrator;
- channel: email;
- transport: SMTP through Spring Mail;
- source model: Lot 5.7.1 `NotificationIntent`;
- orchestration/retry/DLQ: Lot 5.7.2.

SMTP remains an infrastructure concern behind
`OperationalNotificationDeliveryGateway`.

## Ownership

Notification owns:

- logical administrator recipients;
- email address resolution;
- template rendering;
- SMTP provider adaptation;
- provider error classification;
- masking of operational logs.

Payment and Accounting do not depend on SMTP or Spring Mail.

## Recipient resolution

`NotificationIntent` continues to contain only a logical reference:

```text
operations-admin
```

The actual email address is resolved only inside the email infrastructure.

Example configuration:

```yaml
sixpay:
  notification:
    operational:
      email:
        admin-recipients:
          - reference: operations-admin
            email: operations@example.test
            locale: fr
            enabled: true
```

This keeps personal/contact data outside the canonical Notification intent and
allows future LDAP/AD/directory resolution without changing the domain.

## SMTP semantics

A successful `JavaMailSender.send()` is mapped to:

```text
ACCEPTED
```

not:

```text
DELIVERED
```

SMTP acceptance proves only that the configured SMTP infrastructure accepted
the message. Final mailbox delivery requires a future provider-specific
delivery-status mechanism.

## Provider error classification

| Provider/infrastructure condition | Canonical result |
|---|---|
| SMTP send/transient provider error | retryable |
| generic Spring `MailException` | retryable |
| SMTP authentication failure | permanent |
| mail parse/preparation failure | permanent |
| unknown admin recipient reference | permanent |
| invalid template contract | permanent |
| unsupported non-email channel | permanent |

Retryable errors are handled by the bounded retry/DLQ mechanics from Lot 5.7.2.

## Masking

Operational logs may contain:

- notification ID;
- template key;
- masked recipient;
- correlation ID;
- safe error code;
- provider exception class name.

They must not contain:

- raw email address;
- rendered body;
- template variable values;
- SMTP username/password;
- provider exception message;
- Payment account data;
- NIU or customer identity.

Example:

```text
operations@example.test
->
o***s@example.test
```

## Templates

The adapter reuses the versioned templates and allow-lists from Lot 5.7.1.

The renderer requires the variables to exactly match the template contract.
Missing or additional variables are rejected before SMTP.

Current subjects:

- `PAYMENT_POSTED_ADMIN_V1` -> `Paiement comptabilisé`;
- `ACCOUNTING_BATCH_COMPLETED_ADMIN_V1` -> `Batch comptable terminé`.

Bodies remain classpath resources under:

```text
notification/templates/
```

## Configuration

SMTP is disabled by default in standalone mode.

Enable it explicitly with:

```text
SIXPAY_NOTIFICATION_OPERATIONAL_EMAIL_ENABLED=true
```

SMTP connection values use environment variables under the `spring.mail`
configuration.

The historical Partner email pipeline remains untouched and may continue using:

```text
sixpay.notification.email.*
```

The new operational flow uses:

```text
sixpay.notification.operational.email.*
```

The two configurations are intentionally separated.
