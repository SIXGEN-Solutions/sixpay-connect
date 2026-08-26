# CB-1 — Core Banking API Baseline

## Purpose

This document is the authoritative architecture baseline for Core Banking APIs
consumed by SIXPAY CONNECT on `feat/repository-baseline-consolidation-cleanup`.

No API is bank-approved solely because an OpenAPI contract or Java adapter
exists. External approval is a separate gate.

## Status model

### Bank approval

- `APPROVED`: formally approved by the bank/provider.
- `PENDING_BANK_APPROVAL`: SIXPAY baseline exists; bank approval is missing.
- `TO_DEFINE`: no stable baseline exists.

### MVP requirement

- `REQUIRED`: required by the target MVP.
- `OPTIONAL`: used only if the bank confirms support and the project enables it.

`OPTIONAL` is not an approval status.

## Inventory

| Capability | Contract | Owner | Bank approval | MVP |
|---|---|---|---|---|
| Customer discovery | `amplitude-customer-verification-api-v1.yaml` | Customer | `PENDING_BANK_APPROVAL` | `REQUIRED` |
| Customer/KYC/account verification | `amplitude-customer-verification-api-v1.yaml` | Customer | `APPROVED` | `REQUIRED` |
| Payment execution/funds check | `amplitude-payment-posting-api-v1.yaml` | Payment | `APPROVED` | `REQUIRED` |
| Fund reservation/lookup/release | `amplitude-payment-posting-api-v1.yaml` | Payment | `PENDING_BANK_APPROVAL` | `OPTIONAL` |
| Atomic debit + CUT credit posting | `amplitude-payment-posting-api-v1.yaml` | Payment | `APPROVED` | `REQUIRED` |
| Posting lookup | `amplitude-payment-posting-api-v1.yaml` | Payment | `APPROVED` | `REQUIRED` |
| Reversal + reversal lookup | `amplitude-payment-posting-api-v1.yaml` | Payment | `APPROVED` | `OPTIONAL` |
| TFJ/EOD callback + fallback lookup | `amplitude-end-of-day-confirmation-api-v1.yaml` | Accounting / Payment lifecycle | `APPROVED` | `REQUIRED` |

## Signatures to confirm with the bank

### Customer verification

`POST /api/v1/customer-verifications`

Customer discovery:
- `GET /api/v1/customers`
- `GET /api/v1/customers/{customerReference}`
- `GET /api/v1/customers/{customerReference}/accounts`

### Funds check

`POST /api/v1/payment-checks`

### Posting

`POST /api/v1/payment-postings`

A financial command with an uncertain transport outcome is never blindly retried.

### Posting lookup

- `GET /api/v1/payment-posting-lookups/{idempotencyKey}`
- `GET /api/v1/payment-postings/{bankPostingReference}`

At least one authoritative lookup mechanism is mandatory before posting sandbox certification.

### Reversal

- `POST /api/v1/payment-postings/{bankPostingReference}/reversals`
- `GET /api/v1/payment-postings/{bankPostingReference}/reversals/{reversalReference}`

Reversal is `OPTIONAL` until explicitly confirmed by the bank.

### TFJ / EOD

Primary proposal:
`POST <SIXPAY>/webhooks/v1/amplitude/end-of-day-confirmations`

Fallback proposal:
`GET <AMPLITUDE>/api/v1/end-of-day-confirmations`

The bank must confirm whether the real mechanism is callback, query, file/batch,
or another integration mechanism.

## Resolved documentary drift

1. `PENDING_BANK_APPROVAL` is the canonical Amplitude status while provider approval is missing.
2. `OPTIONAL` describes MVP requirement, not approval.
3. `amplitude-payment-verification-api-v1` is obsolete/nonexistent; execution-time funds checking belongs to `amplitude-payment-posting-api-v1.yaml`.
4. `AmplitudeBankingClient` is legacy foundation debt; narrow capability-specific clients are the target.
5. Amplitude owns banking facts; SIXPAY owns SIXPAY Customer enrollment and partner-subscription lifecycle.

## Promotion to APPROVED

Promotion requires traceable bank evidence for endpoint/method, request/response
schema, security, error/status codes, idempotency/retry semantics where
applicable, sandbox URL/certificates, approval date and reference.
