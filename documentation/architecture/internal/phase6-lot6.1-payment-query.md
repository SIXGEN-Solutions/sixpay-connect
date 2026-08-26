# Phase 6 — Lot 6.1 Payment Query Completion

## Purpose

Complete `payment-query-api-v1.yaml` conformance while preserving the Payment
aggregate boundary and the `NamedParameterJdbcTemplate` read model.

## ObservedCustomer association

Customer returns the generated `observedCustomerId` after applying a Payment
observation. The bootstrap anti-corruption adapter preserves this identifier in
the Payment-facing result.

Payment persists only a minimal Payment-owned read-side association:

`payment_observed_customer_link(payment_id, observed_customer_id)`.

Payment never queries `customer_observed_payment`.

## Exit criteria

- `observedCustomerId` filtering is implemented;
- Payment summary/detail expose the linked identifier;
- Payment-owned persistence boundary is preserved;
- contract request validation is aligned;
- Problem Details are returned for query failures;
- `SCOPE_payment.read` remains mandatory.

```text
P6-001 = CLOSED
PAYMENT_QUERY_CONFORMANCE = IMPLEMENTED
NEXT_LOT = 6.2_OBSERVED_CUSTOMER_QUERY_COMPLETION
```
