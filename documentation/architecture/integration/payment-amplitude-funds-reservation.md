# Lot 5.4.2 — Payment Funds Reservation

## Decision

Funds reservation is distinct from the read-only funds check delivered in
Lot 5.4.1.

The existing `FundsGateway` remains unchanged. Lot 5.4.2 adds:

- `FundsReservationGateway`;
- `FundsReservationSnapshot`;
- `AmplitudeFundsReservationAdapter`;
- a dedicated non-retrying HTTP client.

## Safety rule

Reservation is a financial side effect. It is never automatically retried.

A connection failure, timeout, HTTP 429 or HTTP 5xx after request emission is
classified as an unknown outcome. The same idempotency key must be used later
for lookup or controlled replay after the provider contract is confirmed.

## Outcomes

- `RESERVED`: reference and expiry are mandatory;
- `REJECTED`: stable reason code is mandatory;
- `UNKNOWN`: no business rejection is inferred.

## Deferred to Lot 5.4.3

- reservation lookup;
- release/cancel reservation;
- posting against a reservation;
- unknown-outcome reconciliation;
- expiry recovery.
