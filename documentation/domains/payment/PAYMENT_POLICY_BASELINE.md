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

For the MVP, Funds Control is a logical Payment gate, not a standalone read-only
Core Banking request. The eight mandatory checks are evaluated atomically by the
same protected T0 Core Banking financial command that resolves/uses the configured
Treasury destination and executes customer debit plus Treasury credit.

A previous positive funds check cannot be treated as a reservation because other
banking channels may change the same customer account concurrently.

Target contract:
`documentation/contracts/amplitude/amplitude-payment-posting-api-v1.yaml`.
Its registry lifecycle, approval and generation-policy fields remain authoritative.

## Posting authorization profile

The existing `APPROVED_FOR_POSTING` and `POSTING_PENDING` lifecycle names are
retained. In the aligned MVP semantics, they prepare and track the T0 financial
execution command rather than a separate post-Funds-Control banking transaction.

No successful T0 outcome may be inferred before Core Banking confirms the
financial effect or an authoritative lookup resolves an uncertain outcome.

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

The end-of-day Accounting/TFJ lifecycle is distinct from T0 financial success.

Accounting selects eligible financially successful Payments, uses authoritative
TRESOR PAY status evidence, constitutes a batch, and submits it to the Core
Banking accounting capability. Core Banking owns generation and posting of the
accounting entries.

TFJ/accounting statuses may be `INTEGRATED` or `FAILED`, but an Accounting/T+1
failure does not retroactively make a successfully executed T0 Payment unpaid.

No automatic reversal is inferred solely from an Accounting/T+1 failure.
Reversal of the financial T0 effect remains a separate explicitly authorized
financial capability.

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
