# Customer Observation — Implementation Closure

## Scope

This package closes the Customer Observation implementation phase delivered
through Lots 4.6, 4.7 and 4.8.

The capability now covers:

- durable Payment-owned projection events;
- stable Outbox serialization and dispatch;
- Customer-owned observed-customer projection;
- idempotent replay and stale-event handling;
- internal read-only query API;
- signed cursor pagination;
- append-only audit;
- bounded metrics and safe logs;
- explicit retry classification and bounded backoff;
- Actuator health indicators;
- end-to-end acceptance and operational documentation.

## Ownership boundaries

Customer owns:

- the observed-customer projection;
- the internal query language and views;
- the query API;
- projection/query audit records;
- Customer persistence and query adapters;
- Customer metrics, health and resilience components.

Customer does not import Payment, Payment JPA entities, Amplitude adapters or
banking response payloads.

Bootstrap remains the only inter-module composition point between Payment and
Customer.

## Acceptance command

From `backend/`:

```bash
mvn -pl customer,bootstrap -am clean verify
```

Repository-level validation:

```bash
./scripts/validation/validate-customer-observation-phase.sh
```

On Windows PowerShell:

```powershell
.\scripts\validation\validate-customer-observation-phase.ps1
```

## Evidence

The complete acceptance evidence is defined in:

- `E2E-ACCEPTANCE-MATRIX.md`;
- `PHASE-CLOSURE-CHECKLIST.md`;
- Maven Surefire/Failsafe reports;
- Flyway validation output;
- OpenAPI lint output;
- architecture-test reports.
