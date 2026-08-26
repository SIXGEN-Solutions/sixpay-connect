# Lot 5.4.5 Implementation Record

Implemented:

- dedicated Amplitude posting-status client;
- lookup by idempotency key;
- fallback lookup by bank reference;
- mapping to existing `PostingOutcomeSnapshot`;
- authoritative lookup observation channels;
- application status-query service;
- reconciliation classification service;
- coexistence with the historical lookup adapter;
- configuration, architecture tests, contract and runbook.

Deferred:

- persistent reconciliation queue;
- scheduled polling;
- operator-facing reconciliation API;
- automatic case assignment;
- SLA alerts.

Those concerns require the repository's final operational persistence and
scheduler conventions. The implemented service is deterministic and ready to
be called by that later orchestration.
