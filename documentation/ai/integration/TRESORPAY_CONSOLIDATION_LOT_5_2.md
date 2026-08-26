# TresorPay Consolidation — Lot 5.2

## Implemented

- Payment uses the shared Lot 5.1 correlation constants and resolver;
- idempotency header length aligned to 150;
- configurable mTLS, OAuth2 and compatibility API key policy;
- JWT audience and required-scope verification;
- authenticated partner and `LoginName` consistency;
- timestamp/nonce replay protection;
- callback hostname allowlist;
- fixed-window per-partner rate limiting;
- stable public TresorPay error codes;
- structured access audit without payload or account logging;
- callback signing-key provider abstraction;
- detached JWS test;
- security, callback and error supplementary contracts;
- simulated request/response fixtures;
- onboarding and runbook documentation.

## Temporary components

The following components are intentionally replaceable:

- `InMemoryTresorPayNonceStore` must become a shared persistent or Redis-backed
  store before horizontal production deployment;
- `FixedWindowTresorPayRateLimiter` can be replaced by gateway or Redis rate
  limiting without changing the controller;
- `PemCallbackSigningKeyProvider` can be replaced by Vault/KMS/HSM;
- sandbox values remain environment configuration.

## Dependencies

Apply Lot 5.1 before Lot 5.2. This lot imports:

- `CorrelationIdResolver`;
- `IntegrationHttpHeaders`;
- the Maven `integration` module.

## Validation

```bash
cd backend
mvn -pl payment -am test
mvn -pl payment -am verify
```
