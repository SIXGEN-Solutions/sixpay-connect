# Operational Notification operations runbook

## 1. Status interpretation

### PENDING
Durable notification waiting for its first dispatch.

### DISPATCHING
Claimed by a worker. Do not manually replay.

### ACCEPTED
SMTP/provider accepted the request. This does not prove mailbox delivery.

### DELIVERED
Final delivery is known. Terminal.

### FAILED_RETRYABLE
The worker will retry when `nextAttemptAt` is reached.

### FAILED_PERMANENT
Terminal. Do not replay automatically. Fix the underlying configuration or
contract first and use a future privileged remediation procedure if approved.

### DEAD_LETTERED
Automatic retries are exhausted. Eligible for controlled replay after the
underlying cause has been corrected.

## 2. Controlled replay checklist

Before replaying a `DEAD_LETTERED` notification:

1. identify the safe error code;
2. review attempt history;
3. verify the provider/configuration problem is corrected;
4. verify the logical recipient still exists;
5. provide an operator reference;
6. provide a concrete reason;
7. replay the existing notification, never create a replacement intent.

The replay keeps the original deduplication key and total attempt history.

## 3. What not to replay

Do not replay:

- `PENDING`;
- `DISPATCHING`;
- `ACCEPTED`;
- `DELIVERED`;
- `FAILED_RETRYABLE`;
- `FAILED_PERMANENT`.

## 4. Metrics to monitor

Primary operational signals:

- count of `DEAD_LETTERED`;
- count of `FAILED_PERMANENT`;
- count of due notifications;
- oldest due age;
- replay count.

Suggested alert candidates for a later deployment profile:

- `DEAD_LETTERED > 0`;
- oldest due age above the agreed delivery SLO;
- sustained growth in `FAILED_RETRYABLE`.

Thresholds are not hard-coded in this lot.

## 5. Purge

Default policy:

- delivered: 90 days;
- permanent/dead-letter failures: 365 days;
- maximum 500 parent notifications per purge execution.

Never purge non-terminal notifications.

## 6. Safe diagnostics

Allowed:

- notification ID;
- source ID/reference;
- logical recipient reference;
- status;
- correlation ID;
- template key;
- safe error code;
- attempt/replay timestamps.

Forbidden:

- SMTP password;
- raw recipient email in operational exports;
- rendered email body;
- OAuth/JWT/API credentials;
- raw banking/customer payloads.

## 7. Incident sequence

If notifications stop moving:

1. inspect `due` and `oldest.due.age.seconds`;
2. inspect retry scheduler health;
3. verify DB connectivity;
4. verify SMTP/provider connectivity;
5. inspect safe error codes;
6. correct the cause;
7. allow normal retries to resume;
8. replay only already `DEAD_LETTERED` records that require it.
