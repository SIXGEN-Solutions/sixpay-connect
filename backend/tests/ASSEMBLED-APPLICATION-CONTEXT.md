# SIXPAY CONNECT — 8.3.1 Assembled Application Context

## Objective

Prove that the implemented SIXPAY backend modules compose in one Spring Boot
application context against one PostgreSQL database.

This is an assembly test, not a business E2E scenario.

## Classpath strategy

`bootstrap` is not a global dependency of `backend/tests`.

It is activated only through:

```text
-Passembled-tests
```

This preserves focused IT isolation while allowing a dedicated assembled
context to load all implemented modules.

## Test profile

`application-assembled-test.yml` enables:

```text
PostgreSQL
Flyway
JPA
Customer Observation persistence
Customer Observation query
Spring Security filter-chain composition
internal messaging mode
```

and disables external/customer banking, OIDC, local login behavior, retry
scheduling and outbox execution.

The Customer Observation AES/HMAC values are deterministic test-only fixtures.

## Evidence

`AssembledApplicationContextIT` proves:

```text
Spring web application context starts
SecurityFilterChain is assembled
classpath Flyway migrations coexist
Flyway history is written in schema sixpay
Partner auto-configuration participates
Customer auto-configuration participates
Payment auto-configuration participates
Accounting auto-configuration participates
Security auto-configuration participates
Notification persistence auto-configuration participates
```

## Execution

From `backend/`:

```bash
mvn -pl tests -am \
  -Pfull-tests,assembled-tests \
  -Dit.test=AssembledApplicationContextIT \
  verify
```

Regression check for focused golden flow:

```bash
mvn -pl tests -am \
  -Pfull-tests \
  -Dit.test=GoldenModuleE2EIT \
  verify
```

## Exit criteria

8.3.1 is closed when:

```text
AssembledApplicationContextIT = GREEN
all Flyway migrations coexist
SecurityFilterChain exists
principal module auto-configurations participate
GoldenModuleE2EIT still passes without assembled-tests
```

Any bean collision, missing mandatory property, duplicate migration version,
repository collision, or unsatisfied dependency found here is an assembly
defect and must be corrected before 8.3.2.
