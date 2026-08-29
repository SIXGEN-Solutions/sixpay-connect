# SIXPAY CONNECT — Agent Instructions

These instructions apply to every task performed in this repository.

## Mandatory context

Before analyzing or changing the project:

1. read `ENGINEERING_CONTEXT.md`;
2. read `MASTER_PROMPT_INPUT_MANIFEST.yaml`;
3. load every file listed under `contextSelection.alwaysLoad`;
4. read `MASTER_ENGINEERING_PROMPT.md` last;
5. load only the `loadOnDemand` sources relevant to the requested capability.

Never load documents listed under `excludedHistoricalDocuments` as active
context. Never treat deferred contracts as active MVP contracts.

Follow the source precedence, contract policies and generation permissions
defined by these sources.

## Repository revision

The authoritative branch, tag or commit is provided by the task invocation or
selected by the execution environment.

Before writing files:

- report the current branch and `HEAD`;
- confirm that the manifest baseline commit is an ancestor of `HEAD`;
- use the explicitly requested revision when one is provided;
- preserve unrelated and pre-existing changes;
- do not require `HEAD` to equal a hard-coded commit unless the user explicitly
  requests immutable revision validation.

A synthetic branch name used by Codex Cloud is not, by itself, a failure when
the checked-out commit corresponds to the requested revision.

Before writing files:

- inspect the current branch, commit and worktree;
- confirm that the manifest baseline commit is an ancestor of `HEAD`;
- preserve unrelated and pre-existing changes;
- do not require `HEAD` to equal a hard-coded commit unless the user explicitly
  requests validation of an exact revision.

A synthetic branch name used by Codex Cloud is not, by itself, a failure when
the checked-out commit corresponds to the requested authoritative revision.

## Engineering rules

Analyze the existing implementation before proposing or applying a change.

Work on one bounded capability at a time.

Use `backend/partner` as the structural and implementation-quality reference
for business modules. Do not copy Partner business rules into other domains.

Preserve the modular-monolith boundaries:

- business modules must not access another module's infrastructure, JPA
  entities or repositories;
- `bootstrap` is the composition module and contains no business logic;
- `integration` contains provider-neutral technical capabilities only;
- provider payloads and mappings remain in the owning domain;
- `CustomerSubscription` belongs to `customer`;
- do not introduce a standalone `subscription` module;
- Payment does not own the CustomerSubscription lifecycle.

Resolve contracts through
`documentation/contracts/CONTRACT_REGISTRY.yaml`.

Respect each contract's:

- `lifecycleStatus`;
- `approvalStatus`;
- `generationPolicy`;
- `codeGenerationAllowed`.

Do not invent requirements, endpoints, fields, events, tables, permissions,
dependencies, external systems or execution evidence.

## Authorization and safety

An analysis or review request does not authorize file modifications.

A modification request does not authorize:

- creating or changing branches;
- committing;
- pushing;
- opening or merging a Pull Request;
- deploying;
- changing protected external systems.

Perform these operations only when explicitly requested.

Do not expose or commit secrets, credentials, production data, generated
artifacts, build outputs or test reports.

Stop and request a human decision before an unapproved change to contracts,
schemas, migrations, security, dependencies, module boundaries, CI/CD or
deployment strategy.

## Validation

Run the smallest applicable validation first, followed by the broader gates
required by the change.

Canonical non-Docker validations include:

```bash
cd backend
mvn verify
```

```bash
cd frontend
npm run verify:sixpay
```

Use `npm ci --no-audit --no-fund` first when frontend dependencies are absent
or must be restored from the lockfile.

Run Python gates with the platform-appropriate launcher:

```bash
python3 scripts/<gate>.py
```

On Windows:

```powershell
py scripts\<gate>.py
```

`verify_baseline.py`, `-Pfull-tests` and Testcontainers validations require
Docker when PostgreSQL integration tests are executed.

A gate may be reported as passed only when its command finishes successfully.
Distinguish repository failures from environment blockers.

Before completing a modification, run:

```bash
git diff --check
git status --short
```

Report changed files, executed validations, skipped validations, environment
blockers and remaining risks.

Do not commit or push unless explicitly requested.