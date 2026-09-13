# Payment Module

## Purpose

Payment owns payment business behavior, state transitions, idempotency, audit,
financial execution orchestration and Outbox boundaries.

## Responsibilities

- validate and durably persist payment commands;
- coordinate banking customer/account/KYC verification;
- orchestrate bank-owned confirmation challenges;
- enforce Payment authorization and state invariants;
- build immutable financial-event snapshots;
- submit approved Payment events to Core Banking through Payment-owned adapters;
- recover uncertain external outcomes through authoritative lookup;
- persist Payment state, audit and Outbox atomically.

## Current Payment flow

```text
TRESOR PAY payment request
-> durable Payment persistence
-> Customer Verification / Core Banking
-> verified banking evidence
-> bank-owned confirmation challenge
-> OTP verification
-> AUTHORIZATION_CHECKING
-> Payment-owned authorization checks
-> immutable financial snapshot finalization
-> Core Banking Payment Event execution
-> authoritative recovery when outcome is uncertain
-> post-execution lifecycle / Accounting handoff
```

OTP verification proves customer confirmation only. Financial availability and
atomic debit/credit execution are decided by Core Banking at execution time.

## Payment confirmation ownership

SIXPAY may persist the bank `challengeReference` and normalized status but never
the OTP value. Create, verify, replacement and internal revoke orchestration are
idempotent and uncertain outcomes are recovered authoritatively.

Public confirmation surface:

```text
POST /v1/payments/{paymentReference}/confirmation-challenge
GET  /v1/payments/{paymentReference}/confirmation-challenge
POST /v1/payments/{paymentReference}/confirmation-challenge/verify
POST /v1/payments/{paymentReference}/confirmation-challenge/resend
```

## Customer boundary

Customer Verification remains a Customer capability consumed through reviewed
application/port surfaces. `CustomerSubscription` remains owned by `customer`.

## API

Payment query endpoints are under `/internal/api/v1/payments`. Reporting owns
Payment audit timeline and export APIs.

## Financial execution boundary

Payment owns immutable reduced financial-event and financial-entry snapshots.
Only finalized snapshots cross the execution boundary.

The Payment-owned Core Banking adapter maps those snapshots to the approved
Payment Event contract and submits them through the configured client. Unknown
transport outcomes are recovered through authoritative lookup before retry.

## Boundaries

- Integration owns provider-neutral transport only.
- Provider payloads/mappings remain in the owning domain.
- Customer owns customer verification and CustomerSubscription.
- Accounting owns T+1 accounting and reconciliation.
- Reporting owns immutable Payment audit queries and exports.
- Bootstrap contains no Payment business logic.

## Validation

```bash
mvn -pl payment -am test
mvn -pl payment -am clean verify
mvn -pl payment -am -Pfull-tests clean verify
```

## Persistence ownership

| Table | Purpose |
|---|---|
| `payments` | Payment aggregate and lifecycle |
| `payment_audit` | Immutable Payment audit |
| `payment_outbox_events` | Payment integration events |
| `payment_idempotency` | Idempotency and recovery state |
| `payment_observed_customer_link` | ObservedCustomer link |
| `payment_financial_event_snapshots` | Immutable financial-event snapshot |
| `payment_financial_entry_snapshots` | Immutable financial-entry facts |

## Database baseline

Current Flyway baseline:

```text
V300__payment_baseline.sql
```

