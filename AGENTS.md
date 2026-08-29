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

## Repository identity

The authoritative implementation branch is:

`feat/repository-baseline-consolidation-cleanup`

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