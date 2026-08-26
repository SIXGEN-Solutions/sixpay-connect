# Lot 5.6.1 Implementation Record

Implemented:

- Accounting marker and golden-style top-level package structure;
- canonical Payment-to-Accounting candidate;
- explicit TresorPay status-query evidence;
- provider-status-neutral eligibility policy;
- AUTO/MANUAL daily cut-off abstraction;
- batch and item status model;
- deterministic batch idempotency key;
- Accounting-owned candidate-source port;
- batch lookup port by ID and idempotency key;
- provisional request/response JSON Schemas;
- provisional Accounting Batch API contract;
- unit and architecture tests.

Deferred to 5.6.2+:

- persistence of batches and item assignments;
- Payment composition adapter;
- concrete Accounting Batch API client;
- batch submission;
- status polling;
- reconciliation persistence;
- operational scheduling.

No TFJ or SFTP implementation is introduced.
