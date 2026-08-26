# SIXPAY CONNECT — Repository Hygiene

## Purpose

This document defines the canonical FS-2.9 repository-hygiene policy. The
authoritative branch must describe the current system, not local build output,
empty future structure or superseded delivery scaffolding.

The authoritative implementation branch is
`feat/repository-baseline-consolidation-cleanup`.

## Retention rules

Tracked content must have a current repository purpose. The following are
retained:

- implementation, tests and executable validation gates;
- canonical architecture, requirements and contracts;
- approved reference sources and AI traceability assets;
- phase-branch metadata preserved inside assets explicitly classified as
  historical traceability;
- source-controlled developer configuration that is actively usable;
- runtime contract mirrors required by an owning module.

The `backend/partner` module remains the golden business-module reference.
Hygiene work must not reshape business modules merely to create a new
convention.

## Forbidden tracked artifacts

The canonical tree must not contain:

- build output, test reports, coverage output or dependency directories;
- temporary patch/apply scripts, editor backups or compiled binaries;
- zero-byte files or `.gitkeep` placeholders;
- empty Maven modules or empty future feature skeletons;
- duplicate source/reference documents without an explicit runtime reason;
- references that still identify a superseded implementation branch as
  authoritative.

Git history is the preservation mechanism for removed delivery evidence.

## Explicit duplicate exceptions

The repository-hygiene gate permits only these intentional identical copies:

- canonical Payment OpenAPI contracts mirrored into the Payment runtime
  resources;
- the received TRESOR PAY/Core Banking source document mirrored between its
  requirement-source and architecture-reference classifications;
- `.node-version` and `.nvmrc`, retained for compatible Node version managers.

Any new identical tracked pair requires an explicit policy update.

## Ownership decisions

- Customer enrollment and implemented partner-subscription lifecycle behavior
  remain in `backend/customer`.
- The former empty `backend/subscription` reactor entry is not a bounded
  context and is removed.
- Canonical contracts live under `documentation/contracts`.
- Empty `contracts`, `deployment`, `tools`, Kubernetes and Docker placeholder
  trees are not retained.
- Generated Playwright reports are CI artifacts and are ignored by Git.

## Verification

Run from the repository root:

```bash
python scripts/verify_repository_hygiene.py
```

The gate is also composed by:

```bash
python scripts/verify_baseline.py
```

The gate evaluates tracked files so an engineer's ignored local build output
does not make the repository baseline fail.
