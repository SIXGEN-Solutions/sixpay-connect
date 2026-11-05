# Treasury account resolution — MVP

This feature resolves a single configured Treasury credit account per financial
institution and claim type (`AVI`, `IM7`, `RNF`) after
`PaymentTreasuryAccountResolutionRequested`. No Partner schema, payment initiation
payload, allocation contract or T0 two-entry snapshot is changed.

## Configuration

`backend/bootstrap/src/main/resources/application.yml` defines
`sixpay.payment.treasury.enabled` (false by default). Enable with
`SIXPAY_PAYMENT_TREASURY_ENABLED=true` and configure:

- `TREASURY_BANK_CM_AVI_ACCOUNT_REF`
- `TREASURY_BANK_CM_IM7_ACCOUNT_REF`
- `TREASURY_BANK_CM_RNF_ACCOUNT_REF`

Each value must use canonical `age-ncp-clc`; no production account default is
supplied. The configuration IDs and approved version `v1` are defined in YAML.

The adapter obtains the bank base URL, timeouts, TLS and OAuth2 token provider
from the existing Payment Confirmation banking client. Consequently the
confirmation client configuration must be enabled in the deployment profile.

## Bank behavior

`GET /api/v1/getaccountinfo/{branchid}/{accountnumber}` is read-only and carries
`X-Correlation-ID`. Both `SUCCESS` and `COMPTE_INEXISTANT` may be HTTP 200.
Only `SUCCESS`, non-null `data`, and exact equality of `age`, `ncp` and `clc`
against the configured account are accepted. Unknown codes, mismatches and
transport failures do not create positive resolution evidence and cannot lead
to T0. The application does not interpret bank-native `ife`, `cfe`, `typ`,
`dev` or `sin` as additional business eligibility checks.

A confirmed nonexistent account raises a dedicated exception; this bounded
patch retains existing Outbox retry/dead handling rather than changing the
Payment failure-state policy. Never manually replay an event in `DEAD`.

## Deferred

Direct acceptance of an unverified account is not implemented. It requires
separate policy/security approval. This patch does not change the current
OTP time-window policy or historical payment recovery procedure.

## Regional V1 contract alignment

The authoritative Regional contract operation is `getAccountByBranchAndNumber`
with response `AccountLookupResult`. It uses native `BankAccount` BKCOM fields.
SIXPAY retains its preexisting normalized `BankAccount` schema for other APIs;
`RegionalBkcomBankAccount` is an explicit compatibility alias restricted to the
new endpoint, not an alteration of existing customer-verification operations.
The lookup requires `X-Correlation-ID`; `X-Financial-Institution-Code` is optional
for this operation alone. The existing banking security configuration applies.

The banking contract requires additional BKCOM fields compared with the early
9-field sample response. A bank instance returning only that earlier sample is
not fully compliant with the approved Regional OpenAPI schema.
