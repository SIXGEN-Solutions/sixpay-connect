# Lot 5.4.5 — Posting Status and Reconciliation

## Existing contracts reused

`LookupGateway` already supports:

- lookup by banking idempotency key;
- lookup by bank posting reference.

`PostingOutcomeSnapshot` already authorizes:

- `IDEMPOTENCY_LOOKUP`;
- `BANK_REFERENCE_LOOKUP`.

## Lookup order

```text
banking idempotency key
    -> found: classify authoritative result
    -> not found and bank reference present:
         lookup by bank reference
    -> still not found:
         wait and query again
```

## Reconciliation decisions

- `COMPLETED` -> resolved;
- `REJECTED_NO_FINANCIAL_EFFECT` -> resolved;
- `DEBIT_CONFIRMED_CUT_CREDIT_PENDING` -> wait and query again;
- `REVERSAL_REQUIRED` -> explicit reversal workflow;
- `UNKNOWN` with `QUERY_OUTCOME` -> wait and query again;
- `UNKNOWN` with `OPEN_RECONCILIATION` -> manual reconciliation.

No lookup response causes an automatic replay of posting, release or reversal.
