# FS-1.2 — Full-stack conformance tests

## Objective

Protect the FS-1.1 READY capabilities against cross-layer regressions:

```text
Angular API client
    -> Spring endpoint
    -> application use case/service
    -> repository
    -> PostgreSQL/Testcontainers
```

Covered capabilities: Partner, Payment, Reporting, Customer Management,
Observed Customer and Security User Administration.

The Partner module remains the golden reference.

## Static frontend/contract/backend gate

`frontend/scripts/verify-full-stack-conformance.mjs` verifies:

- Angular capability boundary uses HttpClient;
- contract-backed endpoint prefixes remain declared;
- a Spring source still owns each endpoint prefix;
- the resource remains traceable under documentation/contracts;
- Angular response DTO symbols remain declared;
- FS-1.1 remains registered;
- no HTTP boundary introduces silent API-to-mock fallback.

## Assembled PostgreSQL integration gate

`FullStackContractBackedConformanceIT` runs in the existing backend/tests
assembled harness with real Spring MVC mappings, application context, Flyway,
repository beans, MockMvc and PostgreSQL Testcontainers.

Representative read paths are executed for all FS-1.1 READY capabilities.
The Customer request deliberately runs without optional filters to protect
against PostgreSQL query regressions such as lower(bytea).

## Validation

```bash
cd frontend
npm run verify:integration-contract-backed
npm run verify:full-stack-conformance
npm run lint
npm run test
npm run build:integration
```

```bash
mvn -f backend/pom.xml -pl tests -am -Passembled-tests -DskipITs=false verify
```
