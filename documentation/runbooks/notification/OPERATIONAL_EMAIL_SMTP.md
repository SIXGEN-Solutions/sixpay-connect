# Operational email SMTP runbook

## SMTP_SEND_FAILED / SMTP_TEMPORARILY_UNAVAILABLE

These failures are retryable.

Check:

1. SMTP host reachability;
2. port/TLS connectivity;
3. provider health;
4. network/firewall state.

Do not create another NotificationIntent. Lot 5.7.2 retries the existing
notification identity.

## SMTP_AUTHENTICATION_FAILED

This is classified as permanent for the current attempt lifecycle because
blind automatic retries with the same credentials are not useful.

Check:

1. SMTP username secret;
2. SMTP password secret;
3. SMTP authentication mode;
4. credential rotation;
5. provider account status.

After correction, controlled replay belongs to Lot 5.7.4.

## ADMIN_RECIPIENT_NOT_RESOLVED

Verify the logical recipient reference in:

```text
sixpay.notification.operational.email.admin-recipients
```

Do not replace the reference inside persisted notifications with a raw email.

## EMAIL_TEMPLATE_INVALID / SMTP_MESSAGE_INVALID

Check:

- versioned template resource;
- template allow-list;
- variables produced by 5.7.1;
- configured sender address.

## Safe diagnostics

Allowed:

- notification ID;
- correlation ID;
- template key;
- masked email;
- safe error code;
- provider exception class.

Forbidden:

- raw recipient address in tickets/logs;
- rendered message body;
- SMTP password;
- OAuth/API credentials;
- raw financial/customer data.
