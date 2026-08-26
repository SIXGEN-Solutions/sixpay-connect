# FS-1.1 — Contract-backed full-stack consolidation

## Status

Implemented on `feat/repository-baseline-consolidation-cleanup`.

## Purpose

FS-1.1 locks the already contract-backed frontend capabilities to the real
Spring Boot integration path when Angular runs with the `integration`
configuration.

The consolidation applies to:

- Partner;
- Payment;
- Reporting / Payment Audit;
- Customer Management;
- Observed Customer;
- Security User Administration.

The `partner` capability remains the golden frontend datasource-selection
reference.

## Runtime rule

```text
development / demo / tests
    -> mock datasource MAY be used where the capability already supports it

integration
    -> Angular API client
    -> Spring Boot endpoint
    -> application use case
    -> repository
    -> PostgreSQL

integration
    -X-> mock fallback
```

`environment.integration.ts` MUST configure:

```ts
backend: {
  mode: 'api',
}
```

The Angular `integration` build MUST replace `environment.ts` with
`environment.integration.ts`.

## Capability policy

### Partner, Payment, Reporting, Observed Customer

These capabilities keep the existing golden-style `BackendModeService`
boundary.

Their mock services remain available for development/demo/tests, but
`integration` selects the API datasource.

### Customer Management

Customer Management is already API-only from the Angular service boundary.
No mock fallback is introduced.

### Security User Administration

Security User Administration is already API-only through `HttpClient`.
No mock fallback is introduced.

## UI dependency rule

Components, routes, guards and resolvers MUST NOT import a `*MockService`
directly.

A UI element depends on the capability service. The capability service owns
the development-vs-API datasource decision where such a decision exists.

## Failure policy

An API failure in `integration` MUST remain an API failure.

The frontend MUST NOT silently recover by returning mock data.

Explicit contract semantics such as mapping an HTTP `404` to a domain-level
`null` remain allowed.

## Executable gate

Run:

```bash
cd frontend
npm run verify:integration-contract-backed
```

The gate is automatically executed by:

```bash
npm run start:integration
npm run build:integration
```

The gate verifies:

1. `environment.integration.ts` uses `backend.mode = 'api'`;
2. Angular's `integration` configuration performs the required environment
   file replacement;
3. each READY capability keeps its expected API client/service boundary;
4. Customer Management and Security User Administration remain API-only;
5. no UI layer imports a mock service directly;
6. no READY capability introduces a silent API-to-mock error fallback.

## Out of scope

FS-1.1 does not invent contracts for Accounting, generic Administration,
Incidents or other `TO_DEFINE` capabilities.

Those capabilities must pass the repository integration change gate before
being promoted to the contract-backed full-stack set.

## Definition of done

```bash
cd frontend

npm run verify:integration-contract-backed
npm run lint
npm run test
npm run build:integration
```

Backend validation remains part of the global repository verification:

```bash
mvn -f backend/pom.xml clean verify
```
