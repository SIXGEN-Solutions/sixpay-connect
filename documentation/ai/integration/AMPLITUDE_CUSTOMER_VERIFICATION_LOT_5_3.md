# Lot 5.3 Implementation Record

## Added or consolidated

- Customer module depends on the shared `integration` foundation;
- standard `RestClient` factory reused;
- OAuth2 client-credentials configuration completed;
- Spring Boot SSL bundle configuration completed;
- provisional versioned Amplitude contract;
- configurable success and business-failure codes;
- strict successful-response validation;
- explicit 429 handling with `Retry-After`;
- 401, 403, 404, 409, 429 and 5xx classification;
- empty and malformed response handling;
- MockWebServer transport tests;
- sandbox profile and certification runbook.

## Deliberately unchanged

- Customer domain;
- `BankingCustomerVerificationPort`;
- verification domain checks;
- Payment-to-Customer in-process integration;
- provider-specific mappings remain in Customer infrastructure.

## Circuit breaker

Not added. Bounded timeouts and retries are sufficient until sandbox metrics
justify a circuit breaker.

## Remaining external blockers

- authoritative endpoint;
- provider-approved request and response schemas;
- final functional code table;
- OAuth2 audience and final scope;
- sandbox certificate chain;
- real sandbox test data and network access.

## Validation

```bash
cd backend
mvn -pl integration,customer -am test
mvn -pl integration,customer -am verify
mvn -Pfull-tests -pl customer -am verify
```
