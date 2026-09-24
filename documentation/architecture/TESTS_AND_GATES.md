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

## Global repository closure

The final repository closure command is:

```bash
py scripts/verify_repository_closure.py
```

It composes the active Master Prompt checks, documentation and contract reference validation, the clean-room repository baseline, the real full-stack Partner/Customer/Payment/Accounting/Administration-Identity-Incidents journeys and a final Partner-neutral tracked-content/path scan.

Closure is valid only when the command exits successfully on the selected revision. Static analysis alone is not closure evidence.

Clean-room validation uses disposable PostgreSQL instances and composes
repository validation, fresh-database bootstrap, executable Bootstrap startup,
health verification, Angular integration startup and the configured Playwright
full-stack journeys.

A pre-existing local SIXPAY database is neither read nor required.

### Permanent Partner-neutral architecture gate

```bash
py scripts/verify_partner_naming_eradication.py
```

This permanent non-regression gate scans tracked source content and tracked
file/directory names across backend, contracts, runtime configuration, scripts,
frontend, tests and documentation.

Only the explicitly deferred external-subscription governance references remain
narrowly exempted. Governance files are still scanned and are not globally
excluded.

The gate is part of the canonical repository baseline:

```bash
py scripts/verify_baseline.py
```

This makes Partner-neutral naming a permanent repository invariant rather than
a one-time cleanup check.
