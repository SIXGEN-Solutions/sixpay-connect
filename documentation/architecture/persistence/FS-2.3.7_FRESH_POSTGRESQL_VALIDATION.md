# FS-2.3.7 — Fresh PostgreSQL From-Scratch Validation

**Branch:** `feat/repository-baseline-consolidation`  
**Gate:** `FS-2.3 — Database baseline consolidation`  
**Status:** From-scratch runtime proof  
**Golden module:** Partner

## Purpose

FS-2.3.7 proves that SIXPAY CONNECT can bootstrap from a truly empty PostgreSQL
database using only canonical module-owned Flyway baselines.

```text
fresh PostgreSQL
schema sixpay absent
        ↓
SixpayApplication
        ↓
Spring Boot Flyway
        ↓
V100
V200
V300
V400
V500
V600
V700
V800
        ↓
Hibernate ddl-auto=validate
        ↓
application context active
```

## Forbidden mechanisms

The proof must not use:

- `flyway repair`;
- manual SQL;
- `baselineOnMigrate`;
- `spring.flyway.baseline-on-migrate=true`;
- historical V2026 migration copies;
- Hibernate `ddl-auto=update`;
- Hibernate `ddl-auto=create`;
- Hibernate `ddl-auto=create-drop`.

Git preserves development history. Flyway describes the current deployable
baseline.

## Runtime proof

`FreshPostgreSqlApplicationIT` starts the real `com.sixpay.SixpayApplication`
against a fresh PostgreSQL Testcontainer.

Before Spring refresh, an initializer connects directly to PostgreSQL and proves
that schema `sixpay` does not exist.

No SQL is executed by the test to create that schema.

Spring Boot/Flyway must create it and apply the canonical baselines.

Hibernate is explicitly forced to:

```text
ddl-auto=validate
```

Therefore Flyway must fully create the schema before JPA becomes ready.

## Required canonical baselines

The test requires at least:

```text
100
200
300
400
500
600
700
800
```

The total migration count is not fixed, allowing future V101/V301/etc.

## Application-ready criteria

The test succeeds only if:

- the real Spring ApplicationContext starts;
- the context is active;
- EntityManagerFactory is created;
- Flyway reports V100–V800 as applied;
- no V2026 migration is applied;
- schema `sixpay` exists after startup;
- `sixpay.flyway_schema_history` exists.

## Exit criteria

FS-2.3.7 is DONE when this test is green with no schema preparation outside the
application.

## Decision

```text
empty PostgreSQL
+ canonical module migrations
+ normal Spring Boot Flyway startup
+ Hibernate validate
= deployable SIXPAY persistence baseline
```
