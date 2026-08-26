# Operational Notification retry / DLQ runbook

## FAILED_RETRYABLE

No manual action is required while attempts remain below the configured limit.

The retry worker selects the row when `next_attempt_at <= now`.

## DEAD_LETTERED

A notification reaches `DEAD_LETTERED` only after the bounded retry policy is
exhausted.

Before replay:

1. inspect the safe `last_error_code`;
2. inspect attempt history;
3. verify recipient configuration;
4. verify the provider is healthy;
5. do not create a new NotificationIntent;
6. any future replay must reuse the existing notification identity.

Manual replay mechanics are intentionally deferred to Lot 5.7.4.

## FAILED_PERMANENT

Do not retry automatically.

Typical future classifications include:

- invalid recipient;
- provider rejection that cannot succeed unchanged;
- invalid template/provider request.

## Safety

Never include in an incident ticket:

- OAuth/JWT/API keys;
- SMTP credentials;
- private keys;
- raw bank-account data;
- raw notification provider responses containing personal data.

Use notification ID, source ID, correlation ID and safe error code.
