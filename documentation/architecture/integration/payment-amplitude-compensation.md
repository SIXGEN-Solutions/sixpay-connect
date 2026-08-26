# Lot 5.4.4 — Release or Compensation

## Decision tree

```text
Reservation exists, no debit confirmed
    -> release reservation

Debit or posting financial effect confirmed
    -> explicit reversal with authorization evidence

Outcome uncertain
    -> lookup/reconciliation before any replay
```

Release and reversal are financial commands. Neither command is retried
automatically after request emission.

The historical `AmplitudeReversalAdapter` remains available for existing stubs
and tests. Dedicated HTTP adapters are activated only when no existing gateway
bean is present.
