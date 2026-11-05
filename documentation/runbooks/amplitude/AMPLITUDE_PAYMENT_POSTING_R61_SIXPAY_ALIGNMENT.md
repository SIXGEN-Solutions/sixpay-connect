# SIXPAY — PaymentEvent R6.1 integration alignment

Reference: Regional supplied `regional-core-banking-api-v1(1).yaml`, plus the
explicit decision that bank accounting date is returned as `PaymentEventResult.dco`.

## T0 command

`POST /api/v1/payment-events` with mandatory `X-Correlation-ID` (UUID),
`X-Financial-Institution-Code`, `Idempotency-Key`, `Content-Type: application/json`.
Authentication (OAuth2 + mTLS) is environment-dependent; local insecure HTTP
must never become a production default.

Exact body top-level properties: `paymentReference`, `snapshotVersion`,
`providerEvent`, `requestedAt`. No `providerEntries` (BKMVTI is T1, not T0).
SIXPAY builds body via `AmplitudePaymentEventMapper.toRequest(...)` from the
immutable finalized `PaymentFinancialEventSnapshot`.
`AmplitudePaymentProviderEvent` is a Java class containing all 243 properties
from the supplied Regional `PaymentProviderEvent` schema; only the 10 approved
semantic properties are populated by the mapper. All remaining values remain
null and are omitted during JSON serialization. These aliases map to legacy
bank names: operationCode/OPE, currency/DEV (`001`), nature/NAT,
technicalUser/UTI, debitAmount/MON1, creditAmount/MON2, label/LIB1.
The separate legacy fields (`ope`, `dev`, `nat`, `uti`, `mon1`, `mon2`, `lib1`)
are not populated a second time to avoid introducing potentially conflicting
values without an explicit bank mapping rule. `operationCode` and
`technicalUser` are resolved from Administration. Core Banking owns the
banking event number, accounting date and night-mode calculation.

## Response and uncertainty

SIXPAY reads the bank-owned `dco` accounting date into the financial event
outcome. `COMPLETED` must carry `dco`; unknown/rejected outcomes may not have a
bank-proven date. Never derive it from the client clock or replay an uncertain
financial POST. Resolve uncertainty by payment reference / idempotency key.

**CONTRACT DISCREPANCY — ACTION REQUIRED**: the supplied approved Regional
OpenAPI `PaymentEventResult` does not currently declare `dco`. Although the
user explicitly requested consuming `dco`, Regional contract ownership must
publish/approve the response field consistently for POST and both lookup
operations. This consumer script does not edit the Regional contract.

**Registry drift**: the SIXPAY posting contract registry documentation may
still describe the previous T0 payload with BKMVTI lines. Align the registry
and the physical canonical SIXPAY contract via a separately approved
contract-governance change, not silently in this script.

**Validation**: run focused mapper/transport/adapter/guard tests, Maven verify,
and a sandbox T0 + uncertain outcome/lookup scenario before replaying an outbox
entry. Ensure no prior Core Banking effect for the same idempotency key.
