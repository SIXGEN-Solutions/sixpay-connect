# Lot 5.4.2 Implementation Record

Implemented:

- dedicated reservation application port;
- immutable reservation evidence;
- OAuth2/mTLS HTTP adapter;
- idempotency header;
- strict response validation;
- no automatic retry;
- explicit unknown-outcome exception;
- sandbox profile, contract, architecture note and runbook.

Not implemented:

- lookup;
- release;
- posting;
- reversal;
- compensation.

External confirmation remains required for the endpoint, schemas, functional
codes, idempotency retention, reservation expiry and lookup semantics.
