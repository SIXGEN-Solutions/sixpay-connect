# Amplitude Funds Reservation Sandbox

## Required scenarios

1. reservation success;
2. insufficient funds;
3. blocked account;
4. opposed account;
5. limit exceeded;
6. duplicate same key and same payload;
7. duplicate same key and different payload;
8. timeout after provider commit;
9. HTTP 429 after request emission;
10. HTTP 500/503 after request emission;
11. malformed success response;
12. expired reservation.

## Mandatory evidence

- payment ID;
- banking idempotency key;
- correlation ID;
- masked account;
- provider reservation reference;
- expiry;
- final classified outcome.

Never log raw account references, OAuth tokens, certificates or secrets.
