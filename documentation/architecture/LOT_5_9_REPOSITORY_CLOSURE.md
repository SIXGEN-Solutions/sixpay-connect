# SIXPAY CONNECT — LOT 5.9 Repository Closure

## Status

Current-state closure definition for:

```text
LOT 5.9 — Full-stack / E2E / final gates
5.9.8 — Repository closure
```

This document defines the required closure evidence. It does not replace
executable gates and must never claim a gate passed unless the corresponding
command completed with exit code `0`.

## Authoritative revision

The closure run is performed against the branch, tag or SHA explicitly selected
for the task. The repository baseline commit remains governed by
`MASTER_PROMPT_INPUT_MANIFEST.yaml`.

For the 5.9.8 implementation preparation, the selected revision is:

```text
branch: feat/repository-baseline-consolidation-cleanup
SHA: c94ea72255c40f048303bfd08efb16ccd842d14c
```

## Required closure gates

### Frontend

```bash
cd frontend
npm run verify:sixpay
```

Purpose:

- formatting;
- Partner contract verification;
- lint;
- unit tests and coverage;
- Angular environment policy;
- contract consolidation;
- runtime datasource policy;
- static full-stack conformance;
- integration-contract-backed policy;
- Angular integration/netlify/default builds.

### Backend

```bash
cd backend
mvn verify
```

Purpose:

- backend reactor unit tests;
- architecture tests;
- compilation and packaging verification;
- canonical backend quality gate.

The ordinary `mvn verify` path does not replace the fresh PostgreSQL integration
proof executed by the repository baseline gate.

### Repository baseline

```bash
py scripts/verify_baseline.py
```

Purpose:

- repository hygiene;
- Spring runtime-configuration hygiene;
- feature-flag registry;
- backend canonical verification;
- frontend canonical verification;
- fresh PostgreSQL bootstrap with Flyway through `V802`;
- Hibernate/application startup validation.

### Clean-room

```bash
py scripts/verify_clean_room.py
```

Purpose:

- Docker availability;
- complete repository baseline verification;
- fresh disposable PostgreSQL;
- executable Bootstrap JAR;
- integration profile;
- actuator health;
- Angular integration frontend;
- full-stack Playwright journeys;
- environment teardown without relying on a pre-existing SIXPAY database.

## Evidence rule

A gate status is one of:

```text
PASSED
FAILED_REPOSITORY
BLOCKED_ENVIRONMENT
NOT_EXECUTED
```

`PASSED` is allowed only when the command terminates with exit code `0`.

`FAILED_REPOSITORY` means the command ran and demonstrated a repository defect.

`BLOCKED_ENVIRONMENT` means the command could not establish a repository result
because a required local capability such as Docker, Java, Maven, Node.js or npm
was unavailable.

`NOT_EXECUTED` means no execution evidence exists yet.

## 5.9.8 execution record

| Gate | Command | Status |
|---|---|---|
| Frontend | `cd frontend && npm run verify:sixpay` | `NOT_EXECUTED` |
| Backend | `cd backend && mvn verify` | `NOT_EXECUTED` |
| Repository | `py scripts/verify_baseline.py` | `NOT_EXECUTED` |
| Clean-room | `py scripts/verify_clean_room.py` | `NOT_EXECUTED` |

This table must be updated only from actual command results.

## Closure criteria

LOT 5.9 can be declared repository-validated only when:

1. all four required gates above are `PASSED`;
2. `git diff --check` succeeds;
3. `git status --short` is reviewed and only intended changes remain;
4. no gate result is inferred from static inspection;
5. no contract, migration, security rule, dependency, module boundary, CI/CD or
   deployment strategy is changed merely to force closure.

## Current conclusion

At document creation time, 5.9.8 is prepared for validation but not yet closed.
The authoritative final status must be derived from the executed gates.

