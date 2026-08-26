# SIXPAY CONNECT — Cross-module Integration Test Harness

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.3 — Cross-module Integration Test Harness
8.3.0 — Harness Foundation
```

## Purpose

`backend/tests` owns scenarios requiring several bounded contexts or the assembled application.
Module-owned behavior stays in its owning module. `partner` remains the golden reference.

## Assembly boundary

`bootstrap` is the executable assembly and already composes the implemented SIXPAY modules.
`backend/tests` therefore depends on `bootstrap` instead of recreating a second module graph.

## Initial harness

```text
CrossModulePostgreSqlTestSupport
  PostgreSQL 15 Testcontainer
  Spring datasource dynamic properties

CrossModuleHarnessSmokeIT
  proves PostgreSQL harness connectivity
```

Infrastructure is added only when a scenario requires it. Kafka, Keycloak, WireMock or SMTP are
not introduced globally by the harness.

## Naming

Cross-module integration tests use Failsafe names:

```text
*IT.java
```

Support classes do not use the `IT` suffix.

## Planned sub-lots

```text
8.3.0 Harness Foundation
8.3.1 Assembled Application Context
8.3.2 Payment / Customer Observation
8.3.3 Payment / Accounting / Reporting
8.3.4 Payment / Notification
8.3.5 Hybrid Security Assembly
8.3.6 Pilot Critical Flow Matrix
8.3.7 Final Cross-module Gate
```

## Execution

Targeted harness:

```bash
mvn -pl tests -am -Pfull-tests   -Dit.test=CrossModuleHarnessSmokeIT   verify
```

Full integration suite:

```bash
mvn -Pfull-tests clean verify
```

## Boundaries

The harness SHALL NOT duplicate module tests, move business logic into `backend/tests`,
reach directly across bounded-context repositories, invent unimplemented capabilities, or
replace in-process calls with HTTP when the architecture keeps modules co-deployed.

## Exit

```text
8.3.0 = READY FOR EXECUTION
8.3 = OPEN
```

Next:

```text
8.3.1 — Assembled Application Context
```
