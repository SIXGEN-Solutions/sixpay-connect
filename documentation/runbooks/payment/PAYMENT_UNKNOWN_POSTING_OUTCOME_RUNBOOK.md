# Unknown Payment Posting Outcome Runbook

## Trigger

A Core Banking posting call timed out or returned an ambiguous result.

## Mandatory rule

```text
Never blindly resubmit the financial command.
```

## Procedure

1. Capture Payment reference, correlation ID and original banking idempotency
   key through protected operational tooling.
2. Use the approved `LookupGateway`:
   - lookup by original idempotency key first;
   - lookup by bank posting reference when available.
3. Record the authoritative posting evidence through the Payment application
   workflow.
4. If the outcome remains unknown, keep the Payment in the explicit unknown
   state and escalate to Operations.
5. Reversal is permitted only with approved authorization evidence.

## Prohibited actions

- changing the idempotency key and retrying;
- direct database status updates;
- creating a second Payment for the same external reference;
- treating a network timeout as a failed financial posting.
