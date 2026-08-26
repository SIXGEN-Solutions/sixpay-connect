# Phase 6 — Lot 6.2 ObservedCustomer Query Completion

## Purpose

Consolidate the Customer-owned ObservedCustomer query capability against
`observed-customer-query-api-v1.yaml` without changing the established use-case
boundaries.

The following remain authoritative:

- `SearchObservedCustomersUseCase`
- `GetObservedCustomerUseCase`
- `ListObservedCustomerPaymentsUseCase`
- `ObservedCustomerQueryApiMapper`
- `ObservedCustomerQueryObservation`

## Stable pagination

`snapshotAt` remains an application/read-model invariant but is no longer a
caller-controlled HTTP parameter.

For a first page, the controller establishes the snapshot from the configured
Customer query clock. For a continuation page, the signed HMAC cursor restores
the original snapshot. The cursor still binds sort, page size and query filters.

## Filters

Existing validation remains responsible for:

- non-blank bounded text filters;
- page size 1..200;
- ordered date ranges;
- exact `ObservedPaymentStatus` values;
- Customer-owned sort values.

NIU lookup remains exact through the protected search hash. Legal-name lookup
remains a normalized prefix search.

## Masking

The query layer continues to expose only masked NIU, phone, email and account
references. No unmasked account identifier is returned.

## Errors

Query errors are emitted as `application/problem+json` and include a stable
`code`, `correlationId` and `X-Correlation-ID` response header. Responses 429
and 503 also expose `Retry-After`.

## Security

All three endpoints remain protected by `SCOPE_observed-customer.read`.

## Exit criteria

```text
P6-002 = CLOSED
OBSERVED_CUSTOMER_QUERY_CONFORMANCE = IMPLEMENTED
NEXT_LOT = 6.3_REPORTING_FOUNDATION
```
