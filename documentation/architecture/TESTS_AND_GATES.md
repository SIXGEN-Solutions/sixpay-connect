# SIXPAY CONNECT — Tests and Verification Gates

## Status

Canonical current-state verification architecture.

## Canonical developer commands

Backend:

```bash
cd backend
mvn verify
```

Frontend:

```bash
cd frontend
npm run verify:quality
```

Whole repository:

```bash
py scripts/verify_baseline.py
```

Repository hygiene:

```bash
py scripts/verify_repository_hygiene.py
```

Spring configuration hygiene:

```bash
py scripts/verify_spring_configuration_hygiene.py
```

Master Prompt selection:

```bash
py scripts/verify_master_prompt_input_manifest.py
py scripts/verify_master_engineering_prompt.py
```

## Specialized gates

Specialized verification remains available for focused feedback, including
contract consolidation, runtime datasource policy, full-stack conformance,
integration contract-backed checks, Angular environments, architecture tests,
contract tests and Testcontainers integration tests.

## Backend integration tests

The repository baseline verifier explicitly enables integration tests needed for
fresh PostgreSQL bootstrap proof.

## Fresh PostgreSQL proof

`FreshPostgreSqlApplicationIT` validates bootstrap from an empty PostgreSQL
instance through all current Flyway migrations, Hibernate validation and
application readiness.

No manual SQL, Flyway repair, historical migration copy or
`baselineOnMigrate` workaround is part of the baseline.

## Principle

Canonical verification commands orchestrate existing specialized rules. A
failure should be fixed at the owning specialized test or gate, then the
canonical baseline verifier should be rerun.

## Clean-room reproducibility proof

```bash
py scripts/verify_clean_room.py
```

Clean-room validation uses disposable PostgreSQL instances and composes
repository validation, fresh-database bootstrap, executable Bootstrap startup,
health verification, Angular integration startup and the configured Playwright
full-stack journeys.

A pre-existing local SIXPAY database is neither read nor required.

### Partner contract final-transition gate

```bash
py scripts/verify_partner_contract_transition.py
```

This gate verifies that the Partner Payment contracts are the active approved
MVP contracts, the migrated TresorPay Payment contracts are superseded and
reference-only, deferred TresorPay subscription contracts remain excluded, and
none of the active Partner contracts for the migrated Payment/Accounting capabilities depends on `TRESOR_PAY`; unrelated capabilities such as `CustomerSubscription` remain outside this transition gate.

