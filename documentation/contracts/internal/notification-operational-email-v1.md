# Operational Notification Email Contract v1

## Input

`NotificationIntent` with:

- `channel = EMAIL`;
- supported versioned template key;
- logical SIXPAY administrator recipient reference;
- template variables matching the template catalogue.

## Output

`NotificationDispatchResult`:

```text
status = ACCEPTED
providerReference = null
```

for the current SMTP implementation.

The SMTP adapter does not claim final mailbox delivery.

## Errors

Retryable:

- `SMTP_SEND_FAILED`;
- `SMTP_TEMPORARILY_UNAVAILABLE`.

Permanent:

- `SMTP_AUTHENTICATION_FAILED`;
- `SMTP_MESSAGE_INVALID`;
- `ADMIN_RECIPIENT_NOT_RESOLVED`;
- `EMAIL_TEMPLATE_INVALID`;
- `UNSUPPORTED_NOTIFICATION_CHANNEL`.

Provider error messages are not part of the contract and are not persisted.

## Security

SMTP credentials are configuration secrets and must come from the deployment
environment/secret store.

They are never included in:

- domain objects;
- Notification intents;
- database delivery records;
- attempt error codes;
- logs.
