# FS-2.6 — Tests and Gates Consolidation

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.6 — Tests and gates consolidation`  
**Golden module:** Partner

## Assessment

FS-2.6 was already partially implemented by earlier work.

Existing specialized gates are useful and remain canonical for their own scope:

```text
verify:contract-consolidation
verify:runtime-datasource-policy
verify:full-stack-conformance
verify:integration-contract-backed
verify:angular-environments
verify_feature_flag_registry.py
configuration architecture tests
contract tests
Testcontainers integration tests
```

They are not deleted.

What was missing was a small, stable entry-point surface for developers and AI.

## Canonical developer commands

### Backend

```bash
cd backend
mvn verify
```

This remains the normal backend verification command.

Important: the Maven parent has `skipITs=true` by default, so this command does
not claim to execute every `*IT`.

### Frontend

```bash
cd frontend
npm run verify:sixpay
```

`verify:sixpay` composes:

```text
lint
unit tests
build:all
```

`build:all` already executes the reviewed Angular environment and
integration/contract/runtime/full-stack build gates.

### Whole repository

From the repository root:

```bash
py scripts/verify_baseline.py
```

This is the canonical repository-baseline verification entry point.

## Root verification coverage

The root gate executes:

```text
1. Feature-flag/configuration registry validation
2. Backend `mvn verify`
3. Frontend `npm run verify:sixpay`
4. Fresh PostgreSQL bootstrap proof
```

The fresh PostgreSQL proof explicitly executes:

```text
FreshPostgreSqlApplicationIT
```

with integration tests enabled.

That test proves:

```text
schema absent
  -> Spring/Flyway startup
  -> V100/V200/V300/V400/V500/V600/V700/V800
  -> Hibernate validate
  -> application context ready
```

without historical V2026 migrations or `baseline-on-migrate`.

## Why specialized gates remain

The canonical command is an orchestration layer, not a replacement for focused
developer feedback.

Examples:

```text
working on contracts
  -> npm run verify:contract-consolidation

working on Angular datasource policy
  -> npm run verify:runtime-datasource-policy

working on backend only
  -> mvn verify

preparing repository baseline
  -> py scripts/verify_baseline.py
```

## No duplicated source of truth

The root gate invokes existing commands/tests; it does not reimplement their
rules.

## Exit criteria

FS-2.6 is DONE when:

- `npm run verify:sixpay` exists;
- backend canonical command remains `mvn verify`;
- `scripts/verify_baseline.py` exists;
- specialized gates remain available;
- fresh PostgreSQL bootstrap is explicit;
- the root baseline verifier passes.
