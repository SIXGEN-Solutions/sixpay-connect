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
