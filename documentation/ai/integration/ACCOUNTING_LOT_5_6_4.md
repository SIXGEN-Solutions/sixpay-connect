# Lot 5.6.4 Implementation Record

Implemented:

- separate Accounting submission-state model;
- crash-safe `SUBMITTING` state persisted before POST;
- unknown-outcome persistence;
- idempotency-first reconciliation;
- batch-ID lookup fallback;
- no blind POST retry after non-READY state;
- provider result -> local batch/item reconciliation;
- provider batch/item reference persistence;
- rejection-code persistence;
- reconciliation attempt tracking;
- atomic batch + tracking persistence;
- optimistic versioning on tracking;
- in-place update of existing Accounting batch items;
- PostgreSQL/Flyway tracking schema;
- unit and architecture tests;
- operational runbook.

Important invariant:

A process crash after the submission intent is persisted can cause a batch to
remain unresolved, but cannot cause SIXPAY to automatically duplicate the
financial side effect.

Deferred:

- operator endpoint/UI for manual reconciliation;
- scheduled reconciliation runner and cadence;
- alert thresholds for long-lived unresolved batches;
- explicit policy for releasing a rejected Payment into a corrected batch.
