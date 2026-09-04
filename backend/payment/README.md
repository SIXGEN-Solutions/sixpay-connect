# Payment Module

## Purpose

The Payment module owns payment business behavior, state transitions,
idempotency, audit and Outbox boundaries.

## Responsibilities

- accept and validate payment commands;
- durably persist a Payment before any Core Banking call;
- coordinate banking customer/account/KYC verification results;
- orchestrate the bank-owned Payment confirmation challenge lifecycle;
- enforce Payment invariants and legal state transitions;
- persist payment state, audit records and Outbox records atomically;
- expose Payment query and confirmation capabilities;
- reconcile external outcomes without blind financial replay.

## Current Payment flow through customer confirmation

The implemented MVP path up to successful OTP confirmation is:

```text
TRESOR PAY payment request
-> Payment durably persisted
-> PaymentReceived relayed from Payment outbox
-> BANKING_VERIFICATION_PENDING
-> Customer Verification / Core Banking invocation
-> banking evidence persisted in Payment
-> VERIFIED -> PENDING_CONFIRMATION
-> create bank-owned confirmation challenge
-> ACTIVE challenge persisted in Payment state
-> optional read / resend-replace operations
-> OTP verification by La Regionale / Amplitude
-> VERIFIED challenge persisted
-> AUTHORIZATION_CHECKING
```

A banking verification outcome other than `VERIFIED` must not create or send an
OTP challenge.

Successful OTP verification proves customer confirmation only. It does not
prove sufficient funds, successful debit, CUT credit or financial finality.
Funds Control and posting are later capabilities.

## Payment confirmation ownership

Payment owns the business orchestration and the current challenge snapshot bound
to the Payment. La Regionale / Amplitude remains system of record for OTP and
bank challenge state.

SIXPAY:

- may persist the bank `challengeReference` and normalized challenge status;
- never persists the OTP value;
- never returns the OTP after verification;
- uses idempotent create, verify, replacement and internal revoke orchestration;
- performs authoritative recovery after an uncertain create/replacement/revoke
  outcome instead of blindly repeating a bank command.

The public TRESOR PAY confirmation surface is exactly:

```text
POST /v1/payments/{paymentReference}/confirmation-challenge
GET  /v1/payments/{paymentReference}/confirmation-challenge
POST /v1/payments/{paymentReference}/confirmation-challenge/verify
POST /v1/payments/{paymentReference}/confirmation-challenge/resend
```

There is no public revoke endpoint. Revocation is an internal Payment
orchestration step used only when an ACTIVE challenge must be abandoned before a
definitive pre-confirmation terminal transition. A VERIFIED challenge is not
revoked by that path.

Authoritative contracts:

- `documentation/contracts/tresorpay/tresorpay-payment-confirmation-api-v1.yaml`;
- `documentation/contracts/amplitude/amplitude-payment-confirmation-api-v1.yaml`.

## Customer boundary

Customer Verification remains a separate Customer capability consumed by
Payment through reviewed application/port surfaces. Payment does not access
Customer infrastructure, JPA entities or repositories.

Customer Verification provides the banking customer/account/KYC facts and
canonical banking references required before Payment confirmation. It does not
manage Payment confirmation, Funds Control, posting or Payment state.

The Payment completion work does not change Customer ownership or introduce a
Payment-owned Customer persistence model. `CustomerSubscription` remains owned
by `customer`.

Authoritative banking verification contract:

`documentation/contracts/amplitude/amplitude-customer-verification-api-v1.yaml`.

## API

The module exposes Payment query endpoints under:

    /internal/api/v1/payments

It also exposes the approved TRESOR PAY Payment confirmation endpoints described
above.

Payment audit timeline and audit export endpoints are owned by Reporting and
are documented by the corresponding internal contracts.

## Boundaries

- Integration owns provider-neutral transport only.
- Provider payloads and mappings remain in the owning domain.
- Customer owns customer verification and CustomerSubscription.
- Accounting owns accounting batches and reconciliation.
- Reporting owns immutable Payment audit queries and exports.
- Payment does not manage external TRESOR PAY subscriptions.
- Bootstrap composes modules and contains no Payment business logic.

## Structure

The module follows the Partner golden-module layering and dependency direction:

- api;
- application;
- domain;
- infrastructure;
- configuration;
- events.

The same Partner invariants apply structurally: domain code is framework-free,
application logic depends on ports rather than adapters, controllers do not
manipulate JPA entities/repositories, persistence adapters map explicitly, and
business mutations remain inside the Payment transaction boundary.

Partner business rules are not copied into Payment.

## Next development boundary

The next Payment completion work starts from `AUTHORIZATION_CHECKING` after a
successfully VERIFIED confirmation challenge.

The canonical Payment policy requires a fresh execution-time Funds Control
before posting. This capability is distinct from Customer Verification and must
use the active contract registry entry for
`amplitude-payment-posting-api-v1`.

That registry entry is currently `ACTIVE_MVP` and `APPROVED`, but its generation
policy is `REFERENCE_ONLY` and `codeGenerationAllowed` is `false`. It therefore
must not be used to generate or modify posting/Funds Control implementation until
its governance status explicitly permits generation.

## Validation

From backend:

    mvn -pl payment -am test
    mvn -pl payment -am clean verify
    mvn -pl payment -am -Pfull-tests clean verify

The full-tests command requires Docker when PostgreSQL integration tests are
selected.

## Persistence ownership

Payment owns these production tables:

| Table | Purpose |
|---|---|
| payments | Payment aggregate and lifecycle, including the current confirmation challenge snapshot |
| payment_audit | Immutable Payment audit |
| payment_outbox_events | Payment integration events |
| payment_idempotency | Command idempotency and replay data |
| payment_observed_customer_link | Link to an ObservedCustomer projection |

Payment does not own Customer, CustomerSubscription, Accounting or Reporting
tables.

Schema:
backend/payment/src/main/resources/db/migration/V300__payment_baseline.sql
