# CB-1 — Amplitude Bank Signature Confirmation Checklist

All items remain `PENDING_BANK_APPROVAL` until traceable bank/provider evidence exists.

## Customer verification
- [ ] Confirm `POST /api/v1/customer-verifications`.
- [ ] Confirm request/response fields and identifiers.
- [ ] Confirm NIU/KYC semantics.
- [ ] Confirm business-negative vs technical-error mapping.
- [ ] Confirm OAuth2 token URL, scopes/audience and mTLS.
- [ ] Confirm timeout, rate-limit and freshness rules.

## Customer discovery
- [ ] Confirm `GET /api/v1/customers`.
- [ ] Confirm `GET /api/v1/customers/{customerReference}`.
- [ ] Confirm `GET /api/v1/customers/{customerReference}/accounts`.
- [ ] Confirm supported search keys and masking/persistence rules.

## T0 Payment event
- [x] Confirm `POST /api/v1/payment-events`.
- [x] Confirm SIXPAY prepares the provider Payment event and its accounting lines.
- [x] Confirm SIXPAY persists reduced immutable snapshots, not the complete historical provider schema.
- [x] Confirm Core Banking owns authoritative execution-time controls and debit/credit atomicity.
- [x] Confirm both authoritative lookups: Payment reference and original Idempotency-Key.
- [x] Confirm OAuth2 Client Credentials + mTLS application security.
- [ ] Confirm the exact provider field subset/code tables required for the SIXPAY bkeve/bkmvti-equivalent payload.
- [ ] Confirm amount/currency precision and provider date/code semantics.

## T1 Accounting
- [x] Confirm SIXPAY reuses immutable T0 financial-entry snapshots for T1.
- [x] Confirm Core Banking validates and effectively posts/accounts submitted lines.
- [ ] Confirm physical Accounting API endpoint and final batch/line wire schema.
- [ ] Confirm cut-off/business-date and reconciliation semantics.

## Fund reservation — OPTIONAL
- [ ] Confirm reservation support, expiry, lookup, capture and release.

## Reversal — OPTIONAL
- [ ] Confirm reversal support, time window, full/partial semantics, reason codes, authorization, idempotency and lookup.

## TFJ / EOD
- [ ] Confirm actual mechanism: callback, query, file/batch or other.
- [ ] Confirm cutoff/business date and matching keys.
- [ ] Confirm final status codes.
- [ ] Confirm callback security/replay behavior if applicable.
- [ ] Confirm fallback query and retention if applicable.
- [ ] Confirm duplicate/ambiguous/conflicting result handling.

## Approval evidence
For each approved capability record the bank/provider document reference,
version/date, approver or meeting reference, sandbox URL, security/certificate
requirements, differences from the provisional SIXPAY contract and final commit SHA.
