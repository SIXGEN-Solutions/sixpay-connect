# Lot 5.4.3 Implementation Record

Implemented:

- dedicated posting client and adapter;
- OAuth2 client credentials and mTLS;
- mandatory banking idempotency key;
- strict response validation;
- mapping to existing `PostingOutcomeSnapshot`;
- no automatic retry;
- explicit unknown-outcome exception;
- sandbox configuration, contract, architecture test and runbook.

Deferred:

- concrete lookup implementation;
- reconciliation scheduler;
- reversal execution;
- compensation.

External confirmation remains required for the real endpoint, provider schemas,
functional codes, idempotency retention and sandbox scenarios.
