# SIXPAY CONNECT — Tests and Verification Gates

## Status

Canonical current-state verification architecture.

This document absorbs the durable conclusions of FS-2.6.

## Canonical developer commands

Backend:

```bash
cd backend
mvn verify
```

Frontend:

```bash
cd frontend
npm run verify:sixpay
```

Whole repository:

```bash
py scripts/verify_baseline.py
```

Repository hygiene only:

```bash
py scripts/verify_repository_hygiene.py
```

## Specialized gates

Specialized gates remain available for focused feedback and are not replaced by
the canonical orchestration commands.

Examples include:

```text
verify:contract-consolidation
verify:runtime-datasource-policy
verify:full-stack-conformance
verify:integration-contract-backed
verify:angular-environments
verify_repository_hygiene.py
architecture tests
contract tests
Testcontainers integration tests
```

## Backend integration tests

The Maven parent skips ITs by default for the ordinary `mvn verify` path.

The repository baseline verifier explicitly enables integration tests for the
fresh PostgreSQL bootstrap proof.

## Fresh PostgreSQL proof

`FreshPostgreSqlApplicationIT` validates the canonical database bootstrap:

```text
empty PostgreSQL
    -> application/Flyway
    -> V100
    -> V200
    -> V300
    -> V400
    -> V500
    -> V600
    -> V700
    -> V800
    -> Hibernate validation
    -> application ready
```

No manual SQL, Flyway repair, historical migration copy or
`baselineOnMigrate` workaround is part of the baseline.

## Failsafe / Spring Boot executable JAR

Failsafe is configured to load application classes directly from Maven's
compiled output directory so integration tests do not depend on the internal
layout of the repackaged Spring Boot executable JAR.

## Principle

The canonical verification commands orchestrate existing specialized rules.
They do not reimplement those rules.

A failure should be fixed at the owning specialized test/gate, then the
canonical baseline verifier should be rerun.

## Clean-room reproducibility proof

Canonical clean-room command:

```bash
py scripts/verify_clean_room.py
```

The clean-room validation deliberately uses disposable PostgreSQL instances
rather than dropping a developer database.

It composes:

```text
repository baseline verification
    -> backend + frontend + canonical gates
    -> FreshPostgreSqlApplicationIT
    -> empty PostgreSQL + V100..V800 + application context

full-stack functional smoke
    -> new PostgreSQL container
    -> executable Bootstrap JAR
    -> integration profile
    -> actuator health UP
    -> Angular integration frontend
    -> Playwright Partner persistence journey
    -> Playwright Customer persistence journey
```

A pre-existing local SIXPAY database is neither read nor required.
