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

## Funds check
- [ ] Confirm `POST /api/v1/payment-checks`.
- [ ] Confirm available vs ledger balance.
- [ ] Confirm restrictions/opposition/status codes.
- [ ] Confirm limits and amount/currency precision.

## Posting
- [ ] Confirm `POST /api/v1/payment-postings`.
- [ ] Confirm atomic debit + CUT credit.
- [ ] Confirm `Idempotency-Key` semantics.
- [ ] Confirm same-key replay/conflict behavior.
- [ ] Confirm success/rejection/partial/unknown outcomes.
- [ ] Confirm bank posting reference.

## Posting lookup
- [ ] Confirm lookup by idempotency key.
- [ ] Confirm lookup by bank posting reference.
- [ ] Identify authoritative lookup.
- [ ] Confirm not-found/eventual-consistency semantics and retention.

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
