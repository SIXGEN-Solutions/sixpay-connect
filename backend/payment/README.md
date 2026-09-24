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
Partner payment request
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

- Partner/Security own and resolve the authoritative SIXPAY Partner identity; Payment receives only the canonical Partner UUID and represents it through its Payment-owned `CanonicalPartnerIdentity` value object.
- Security resolves authenticated partner credentials to the canonical SIXPAY Partner UUID subject before Payment is invoked.
- Provider login names remain at provider API boundaries and are not Payment business identities.
- Integration owns provider-neutral transport only.
- Provider payloads/mappings remain in the owning domain.
- Customer owns customer verification and CustomerSubscription.
- Accounting owns T+1 accounting and reconciliation.
- Reporting owns immutable Payment audit queries and exports.
- Bootstrap contains no Payment business logic.

### Functional classification

Payment keeps Treasury-payment semantics explicit instead of generalizing
provider vocabulary by renaming it:

- `ClaimType` and the taxpayer identifier are Treasury-payment invariants;
- beneficiary allocation is a Treasury-payment invariant represented by `TreasuryAllocationIntent`;
- `ExternalSubscriptionReference` is opaque provider trace metadata and never a local `CustomerSubscription` identity;
- `applicationId` is partner application metadata, not a financial Payment invariant.

The active Partner API remains responsible for mapping its physical wire
fields to these internal concepts. This classification does not change the
active external contract or the persisted Payment state payload.

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

## Partner integration boundary

Partner is one external Partner integration. It is not part of the Payment
domain model.

The Payment domain and application layers remain provider-neutral. Partner
wire contracts, request/response models and provider mappings are permitted only
at approved external boundaries, including `api/partner/partner` and
Partner-specific Payment infrastructure adapters. Canonical contracts under
`documentation/contracts/partner` may remain provider-specific.

`integration` remains technical and provider-neutral and does not own Partner
payloads or mappings.

## External partner contract evolution

SIXPAY does not expose a generic external Partner Payment contract solely to
anticipate future integrations.

Each concrete external Partner integration may keep its own approved wire
contract and anti-corruption boundary while mapping to the provider-neutral
Payment application API.

A shared SIXPAY Partner Payment contract may be introduced only after at least
two concrete Partner integrations demonstrate a stable common external-contract
need. That decision requires explicit architecture and contract approval and
must not be inferred from internal Payment abstractions.

Until such evidence exists, Partner remains a concrete external integration,
not the template for a universal Partner protocol.

