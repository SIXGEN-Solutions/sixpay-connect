# SIXPAY CONNECT — Phase 3 / Lot 3.8

## Banking Adapters Foundation

This delivery prepares the Core Banking integration boundary without
activating any real bank call.

### Generated ports

- `VerificationGateway`
- `FundsGateway`
- `PostingGateway`
- `LookupGateway`
- `ReversalGateway`

### Contract-limited surface

- Verification: customer/KYC/account verification only.
- Funds: fresh read-only execution check only.
- Posting: atomic debit plus configured CUT credit.
- Lookup: by original idempotency key or bank posting reference.
- Reversal: explicit authorized reversal only.

Fund reservation and release are deliberately excluded.

### Activation

All adapters are conditional on a concrete
`AmplitudeBankingClient` Spring bean. No such implementation is included.
Therefore the application compiles and starts, but no Core Banking call can be
performed accidentally.

The later implementation increment must supply HTTP DTOs, mapping, OAuth2,
mTLS, endpoint configuration, resilience settings and integration tests.
