# CB-2 — Core Banking Client Consolidation

## Decision

SIXPAY no longer exposes or wires a generic `AmplitudeBankingClient`.

Provider-specific clients remain inside the owning business domain and are
split by capability.

## Ownership and client matrix

| Capability | Owning module | SIXPAY port | Provider client |
|---|---|---|---|
| Customer verification | Customer | `BankingCustomerVerificationPort` | `AmplitudeCustomerVerificationClient` |
| Payment account/funds checks | Payment | `VerificationGateway`, `FundsGateway` | `AmplitudeAccountFundsClient` |
| Posting | Payment | `PostingGateway` | `AmplitudePostingClient` |
| Posting lookup | Payment | `LookupGateway` | `AmplitudePostingStatusClient` |
| Reversal | Payment | `ReversalGateway` | `AmplitudeReversalClient` |
| Fund reservation | Payment | reservation port | `AmplitudeFundsReservationClient` |
| Fund release | Payment | release port | `AmplitudeFundsReleaseClient` |
| TFJ/EOD | Accounting / Payment lifecycle | dedicated EOD boundary | dedicated EOD integration |

The shared `backend/integration` module remains provider-neutral and must not
contain Amplitude payloads, mappings or banking business semantics.

## Bank-approved endpoints

Approved operations:

- `POST /api/v1/customer-verifications`
- `POST /api/v1/payment-checks`
- `POST /api/v1/payment-postings`
- `GET /api/v1/payment-posting-lookups/{idempotencyKey}`
- `GET /api/v1/payment-postings/{bankPostingReference}`
- `POST /api/v1/payment-postings/{bankPostingReference}/reversals`
- `POST <SIXPAY>/webhooks/v1/amplitude/end-of-day-confirmations`
- `GET <AMPLITUDE>/api/v1/end-of-day-confirmations`

Approval is recorded per OpenAPI operation. Optional reservation/release
operations are not promoted by CB-2 because they are not part of the supplied
bank-approved list.

## Safety

Read-only operations may use bounded retry according to the integration policy.

Financial commands do not use blind retry after an uncertain transport outcome.
Posting uncertainty is resolved through the approved lookup operations before
any replay decision.

## Legacy removed

The following Payment infrastructure types are removed:

- `AmplitudeBankingClient`
- legacy `AmplitudePostingAdapter`
- legacy `AmplitudeLookupAdapter`
- legacy `AmplitudeReversalAdapter`

`AmplitudeVerificationAdapter` and `AmplitudeFundsAdapter` remain in Payment,
but now depend only on `AmplitudeAccountFundsClient`.

## Exit criteria

- no production source references `AmplitudeBankingClient`;
- Payment ports are wired only through capability-specific clients;
- Customer verification remains owned by Customer;
- provider-neutral integration code remains provider-neutral;
- approved endpoint metadata is synchronized with OpenAPI;
- Payment tests and architecture tests pass.
