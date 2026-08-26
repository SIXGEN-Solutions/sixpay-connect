# Integration Foundation — Lot 5.1 Implementation Record

## Added

- standard HTTP client factory;
- correlation, request ID and trace-context propagation;
- external failure taxonomy;
- safe retry executor;
- shared Micrometer metrics and observations;
- sensitive logging sanitizer;
- JSON serializer;
- Kafka naming, headers, routing and event transport;
- generic Kafka DLQ publisher;
- consumer idempotency executor and persistence port;
- Spring Boot auto-configuration;
- unit and architecture tests.

## Modified

- `backend/integration/pom.xml`;
- `ENGINEERING_CONTEXT.md`.

## Intentionally excluded

- Amplitude and TresorPay payloads;
- domain-specific ports and mappings;
- provider-specific error classifiers;
- processed-message database schema;
- production Kafka topic provisioning;
- certificates and secrets;
- Accounting and Notification consumers.

## Validation

```bash
cd backend
mvn -pl integration -am test
mvn -pl integration -am verify
```

## Acceptance statement

Domains can reuse the shared foundation without moving their business ports,
provider adapters or anti-corruption mappings into `integration`.
