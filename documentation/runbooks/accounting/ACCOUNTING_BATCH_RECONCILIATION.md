# Accounting batch reconciliation runbook

## Batch is OUTCOME_UNKNOWN or SUBMITTING after restart

Do not resubmit.

1. obtain batch ID and idempotency key;
2. lookup by idempotency key;
3. if absent, lookup by batch ID;
4. if found, persist the provider result atomically;
5. if absent, leave the batch unresolved;
6. investigate the Accounting provider before authorizing any manual resubmit.

## Batch is RECONCILIATION_REQUIRED

Inspect item results by `paymentId`.

Allowed operational evidence:

- provider batch reference;
- provider item reference;
- rejection code;
- SIXPAY batch ID;
- SIXPAY payment ID;
- correlation/request IDs.

Never copy OAuth tokens, client secrets, private keys or raw banking payloads
into tickets or logs.

## Batch is REJECTED

Do not automatically create another batch containing the same Payments. The
global `payment_id` uniqueness guard intentionally prevents silent re-batching.
A future explicit remediation workflow must decide whether an item can be
released for a corrected batch.
