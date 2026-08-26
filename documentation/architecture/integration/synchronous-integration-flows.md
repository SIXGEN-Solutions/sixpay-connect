# Synchronous Integration Flows

## 1. Scope

This document covers request/response interactions that participate directly in an immediate business decision.

Kafka must not be introduced into these paths merely because the dependency exists. Internal calls remain Java calls while modules are co-deployed.

## 2. Flow SYN-01 — TresorPay initiates a payment

```text
TresorPay
  -> PaymentCommandController
  -> authentication / authorization / partner isolation
  -> correlation and idempotency
  -> PaymentInitiationUseCase
  -> Payment orchestration
  -> Customer Verification internal adapter
  -> Core Banking financial operations where required
  <- canonical Payment result
  <- versioned HTTP response
```

### Contract

- `documentation/contracts/external/payment-command-api-v1.yaml`
- packaged copy: `backend/payment/src/main/resources/openapi/payment-command-api-v1.yaml`

### Security

- TLS and production mTLS decision;
- bearer token and/or approved partner credential model;
- audience, issuer, scope and partner binding;
- mandatory idempotency key;
- correlation ID;
- payload validation;
- no secrets in request body unless an approved legacy constraint is documented.

### Error rules

- schema/validation error: caller correction required;
- duplicate same payload: replay stored result;
- duplicate conflicting payload: idempotency conflict;
- business decline: stable non-retryable response;
- transient dependency failure: retryable only where safe;
- unknown financial outcome: do not return a false failure; persist recoverable/unknown state.

### Tests

- OpenAPI validation;
- controller and mapper tests;
- security and partner-isolation tests;
- idempotency concurrency/replay tests;
- TresorPay stub;
- complete E2E scenario.

## 3. Flow SYN-02 — Payment invokes Customer Verification

```text
Payment orchestration
  -> CustomerVerificationModuleAdapter
  -> Customer VerifyCustomerUseCase
  -> BankingCustomerVerificationPort
  -> Amplitude customer-verification adapter/client
  <- canonical verification result
  <- Payment banking-verification evidence
```

### Contract

The internal boundary is a Java application contract. The receiving Customer module owns accepted command/result semantics.

### Security

- authenticated execution context propagated from Payment;
- correlation preserved;
- no network credential between co-deployed modules;
- sensitive identifiers minimized in logs and events.

### Error rules

- verification FAIL is a business outcome;
- authentication failure from Amplitude is technical and non-retryable until configuration changes;
- timeout/unavailable may be recoverable;
- invalid/protocol response is quarantined/alerted and must not become a business FAIL.

### Tests

- adapter unit tests;
- technical-failure mapping tests;
- intermodule integration tests;
- Customer provider client tests;
- sandbox certification.

## 4. Flow SYN-03 — Customer Verification calls Amplitude

```text
CustomerVerificationService
  -> RetryingBankingCustomerVerificationAdapter
  -> AmplitudeCustomerVerificationAdapter
  -> mapper
  -> AmplitudeCustomerVerificationClient
  -> OAuth2 token provider
  -> HTTPS/mTLS Amplitude endpoint
```

### Current implementation

The client already sends:

- bearer authorization;
- `X-Correlation-ID`;
- `X-Request-ID`;
- JSON request body.

Configuration already exposes base URL, endpoint path, connection/read timeouts, attempts, backoff, evidence TTL, OAuth2 registration and SSL bundle.

### Required before Lot 5.3 completion

- provider-approved endpoint and payload;
- token endpoint, scopes and audience;
- certificate and trust chain;
- status/error mapping;
- rate limit and retry-after behavior;
- sandbox fixtures.

## 5. Flow SYN-04 — Payment verifies customer/account at Core Banking

```text
Payment
  -> VerificationGateway
  -> AmplitudeVerificationAdapter
  -> capability-specific Amplitude account/funds client
  -> Core Banking
```

### Current state

The legacy `AmplitudeBankingClient` interface remains as foundation debt but is
not the target design. Current integration work uses narrow capability-specific
clients/adapters. Customer/KYC/account verification remains owned by Customer;
execution-time account/funds checks remain owned by Payment.

### Contract data required

- endpoint and operation semantics;
- identity/account lookup keys;
- required checks and result codes;
- evidence timestamps/TTL;
- masking and storage policy;
- business-negative versus technical-failure mapping.

## 6. Flow SYN-05 — Payment controls funds

```text
Payment
  -> FundsGateway
  -> AmplitudeFundsAdapter
  -> AmplitudeAccountFundsClient
  -> Core Banking
```

### Required contract decisions

- whether the operation is balance inquiry, execution eligibility, reservation or a combination;
- amount/currency precision;
- available versus ledger balance;
- opposition and account-state codes;
- idempotency and concurrency behavior;
- validity duration of evidence/reservation;
- timeout budget.

## 7. Flow SYN-06 — Payment posts the financial transaction

```text
Payment
  -> PostingGateway
  -> DedicatedAmplitudePostingAdapter
  -> AmplitudePostingClient
  -> RestAmplitudePostingClient
  -> Core Banking
```

### Mandatory safety rules

- every posting has a stable banking idempotency key;
- timeout after request transmission creates `UNKNOWN`, not automatic failure;
- blind retry is forbidden unless provider idempotency is certified;
- lookup by idempotency key or bank reference resolves unknown outcomes;
- bank reference is persisted atomically with Payment state and audit/outbox effects.

## 8. Flow SYN-07 — Payment resolves posting outcome

```text
Payment reconciliation
  -> capability-specific posting lookup adapter/client
  -> lookup by original idempotency key or bank reference
  -> resolve success / rejection / still unknown
```

### Tests

- found by idempotency key;
- found by bank reference;
- not found;
- inconsistent provider response;
- temporary unavailability;
- repeated reconciliation is idempotent.

## 9. Flow SYN-08 — Payment reverses a posting

```text
Authorized compensation
  -> ReversalGateway
  -> capability-specific Amplitude reversal adapter/client
  -> Core Banking reversal
```

### Required controls

- explicit reversal authorization;
- original bank reference;
- reversal idempotency key;
- reason code;
- no double reversal;
- unknown reversal outcome resolved before another attempt;
- complete audit.

Reversal is an optional capability until the bank explicitly confirms support,
semantics and operational controls.

## 10. Core Banking contract status

The authoritative CB-1 status matrix and bank-confirmation gate are maintained
in:

`documentation/architecture/integration/core-banking-api-baseline.md`

## 11. Internal query flows

Payment, Observed Customer and Payment Audit query APIs are synchronous read-only integrations. They must enforce:

- OAuth2/JWT authorization;
- tenant/partner isolation;
- object-level access;
- cursor validation;
- rate limits;
- masking and audit where sensitive;
- no write-side repository access from query controllers.
