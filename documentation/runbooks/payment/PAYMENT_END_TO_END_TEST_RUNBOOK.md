# Payment End-to-End Test Runbook

## Local prerequisites

- Java 21
- Docker
- PostgreSQL Testcontainers support

## Execute

From `backend/`:

```powershell
mvnw.cmd clean verify -Pfull-tests -pl payment -am
```

To execute the E2E test directly:

```powershell
mvnw.cmd -pl payment -Dtest=PaymentEndToEndIntegrationIT test
```

## Expected chain

```text
PaymentReceived
PaymentPostingCompleted
AccountingIntegrationConfirmed
PaymentFinalResultAvailable
```

Accounting must observe the posting-completed event before Notification
observes the final-result event.

## Failure diagnosis

### ApplicationContext failure

Verify conditional beans and provide an empty `CurrentUserProvider` in the
test context.

### Flyway or schema failure

Verify all Payment migrations run and PostgreSQL 15 is available.

### Missing PostingGateway

Verify a test `AmplitudeBankingClient` bean exists so the conditional
Amplitude adapter is activated.

### Unexpected event order

Order by `occurred_at`; do not depend on repository `findAll()` ordering.

### Docker unavailable

The test is integration-only and must not be replaced with H2.
