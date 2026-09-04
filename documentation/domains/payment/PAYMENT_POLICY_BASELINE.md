# SIXPAY CONNECT — Payment Policy Baseline

## Status

- Domain owner: `payment`
- Baseline: `payment-mvp/v1`
- Effective from: `2026-08-31T00:00:00Z`
- Decision reference: `PAYMENT_COMPLETION:R5_POLICY_BASELINE`
- Classification: current-state canonical domain policy
- Runtime implementation: `backend/payment/src/main/java/com/sixpay/payment/configuration/PaymentMvpPolicyConfiguration.java`

## Purpose

This document is the canonical business-policy reference for the SIXPAY Payment MVP.
Core Banking / Amplitude remains system of record for banking facts. Core Banking
APIs do not redefine SIXPAY Payment policy: they expose sufficient facts, outcomes,
timestamps and canonical references for SIXPAY to evaluate this baseline.

Environment-specific URLs, certificates, OAuth clients, timeouts, circuit breakers
and transport settings are not Payment business policy.

## Workflow position

```text
Payment request durably persisted
-> PaymentReceived relayed from Payment outbox
-> BANKING_VERIFICATION_PENDING
-> PaymentBankingVerificationRequested durably persisted
-> event relayed after commit
-> Customer Verification / Core Banking invocation
-> banking evidence persisted in Payment
-> VERIFIED may progress to PENDING_CONFIRMATION
-> REJECTED or INDETERMINATE must not create/send an OTP challenge
```

No Core Banking call is part of the Payment initiation persistence transaction.

## Evidence temporal policy

Maximum future clock skew: `2 minutes`.

| Evidence category | Maximum age |
| --- | ---: |
| AUTHORIZATION | 5 minutes |
| BANKING_VERIFICATION | 5 minutes |
| FUNDS_CONTROL | 2 minutes |
| TREASURY_RESOLUTION | 30 minutes |
| POSTING_OUTCOME | 24 hours |
| TFJ_CONFIRMATION | 48 hours |
| REVERSAL_OUTCOME | 24 hours |

## Banking Verification profile

All modeled checks are mandatory for `payment-mvp/v1`:

1. `CUSTOMER_EXISTS`
2. `FINANCIAL_INSTITUTION_MATCHES`
3. `NIU_MATCHES`
4. `IDENTITY_MATCHES`
5. `ACCOUNT_EXISTS`
6. `ACCOUNT_BELONGS_TO_CUSTOMER`
7. `ACCOUNT_IS_ACTIVE`
8. `ACCOUNT_NOT_BLOCKED`
9. `ACCOUNT_NOT_OPPOSED`
10. `REQUIRED_KYC_PRESENT`
11. `REQUIRED_KYC_VERIFIED`

A positive progression requires all mandatory checks to be satisfied by fresh
banking evidence. Verified evidence carries canonical `customerReference` and
`accountReference`.

Physical contract:
`documentation/contracts/amplitude/amplitude-customer-verification-api-v1.yaml`.

## Funds Control profile

All modeled execution-time checks are mandatory:

1. `ACCOUNT_EXISTS`
2. `ACCOUNT_ACTIVE`
3. `DEBIT_ALLOWED`
4. `CURRENCY_SUPPORTED`
5. `AVAILABLE_FUNDS_SUFFICIENT`
6. `PER_TRANSACTION_LIMIT_NOT_EXCEEDED`
7. `DAILY_LIMIT_NOT_EXCEEDED`
8. `OTHER_APPLICABLE_LIMITS_NOT_EXCEEDED`

Funds Control is distinct from Customer Verification and occurs later, after
successful customer confirmation. Customer Verification is not proof of
sufficient funds.

Target contract:
`documentation/contracts/amplitude/amplitude-payment-posting-api-v1.yaml`.
Its registry lifecycle, approval and generation-policy fields remain authoritative.

## Posting authorization profile

Posting is allowed only from `APPROVED_FOR_POSTING`. SIXPAY requires fresh Funds
Control evidence and a resolved Treasury account before posting.

## Financial outcome evidence hierarchy

Increasing authority:

1. `DIRECT_RESPONSE`
2. `IDEMPOTENCY_LOOKUP`
3. `BANK_REFERENCE_LOOKUP`
4. `UNIQUE_TFJ_MATCH`

Increasing conclusiveness:

1. `INDETERMINATE`
2. `PARTIAL`
3. `CONCLUSIVE`
4. `FINAL`

A timeout or transport failure after a financial command does not prove failure.
Recovery uses authoritative lookup; blind financial replay is forbidden.

## TFJ and reversal policy

TFJ terminal statuses modeled for the MVP are `INTEGRATED` and `FAILED`.
A failed TFJ outcome may require `REVERSAL_REQUIRED`. Reversal requires
`APPROVED_RUNBOOK` authorization and remains subject to the active Core Banking
contract and programme enablement.

## Failure classification policy

- `BUSINESS_REJECTION` -> `NOT_RETRYABLE`
- `SECURITY_REJECTION` -> `NOT_RETRYABLE`
- `TECHNICAL_FAILURE` -> `SAFE_RETRY`, `RECOVERY_EVENT_REQUIRED`, `OPERATOR_ACTION_REQUIRED`
- `UNCERTAIN_EXTERNAL_OUTCOME` -> `AUTHORITATIVE_LOOKUP_REQUIRED`
- `INTEGRATION_CONFLICT` -> `OPERATOR_ACTION_REQUIRED`
- `TREASURY_RECONCILIATION_FAILURE` -> `RECOVERY_EVENT_REQUIRED`, `OPERATOR_ACTION_REQUIRED`

## Result intent policy

- `REJECTED` -> `IMMEDIATE_REJECTED`
- `FAILED` -> `IMMEDIATE_FAILED`
- `DEBIT_CONFIRMED` -> `IMMEDIATE_PROCESSING`
- `POSTING_OUTCOME_UNKNOWN` -> `IMMEDIATE_PROCESSING`
- `POSTED_PENDING_TFJ` -> `IMMEDIATE_POSTED_PENDING_TFJ`
- `REVERSAL_REQUIRED` -> `IMMEDIATE_REVERSAL_REQUIRED`
- `TREASURY_INTEGRATED` -> `FINAL_TREASURY_INTEGRATED`
- `REVERSED` -> `REVERSAL_REVERSED`

## Core Banking implementation obligations

Future Core Banking implementation uses this document together with the active
physical contract and `CONTRACT_REGISTRY.yaml`.

It must not reduce mandatory checks without an approved Payment policy change,
merge Customer Verification with Funds Control, call Core Banking inside the
initial Payment persistence transaction, replace canonical bank references with
SIXPAY-generated identifiers, hard-code environment configuration as business
policy, or generate from a contract whose registry entry forbids generation.

If Core Banking cannot provide a required fact, the gap must be resolved
explicitly; it must not be silently treated as a successful check.

## Versioning

Any semantic change to mandatory checks, freshness windows, outcome hierarchy,
failure dispositions or progression rules requires a new reviewed policy version.
