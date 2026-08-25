# SIXPAY CONNECT — Architecture Documentation

This directory is the canonical architecture source for SIXPAY CONNECT,
subject to the source-of-truth precedence defined in `ENGINEERING_CONTEXT.md`.

Architecture documentation must describe the **current repository baseline**.
Phase/audit documents may exist temporarily as consolidation evidence, but they
are not the preferred navigation surface once their conclusions are absorbed.

## Canonical architecture map

| Concern | Canonical document/location |
|---|---|
| Architecture navigation | `documentation/architecture/README.md` |
| Module boundaries | `documentation/architecture/MODULE_BOUNDARIES.md` |
| Runtime/configuration architecture | `documentation/architecture/CONFIGURATION_ARCHITECTURE.md` |
| Tests and verification architecture | `documentation/architecture/TESTS_AND_GATES.md` |
| Integration landscape | `documentation/architecture/integration/integration-landscape.md` |
| Async integration flows | `documentation/architecture/integration/asynchronous-integration-flows.md` |
| Integration error taxonomy | `documentation/architecture/integration/integration-error-taxonomy.md` |
| Core-banking integration baseline | `documentation/architecture/integration/core-banking-api-baseline.md` |
| Internal architecture | `documentation/architecture/internal/` |
| Feature-flag registry | `documentation/architecture/configuration/FEATURE_FLAG_REGISTRY.yaml` |

## Golden business-module reference

```text
backend/partner
```

remains the structural and implementation reference for business modules.

## Transitional evidence

The following families are considered consolidation evidence, not preferred
current-state entry points:

```text
documentation/architecture/module-boundaries/FS-2.4.*
documentation/architecture/configuration/FS-2.5.*
documentation/architecture/FS-2.6_*
documentation/architecture/FS-2.7.*
```

Their conclusions must be absorbed into canonical documents before any later
archive/delete decision.

## Binary architecture documents

Existing `.docx` architecture documents remain reference candidates until
FS-2.7 semantic review determines whether they are:

```text
KEEP_CANONICAL
MERGE_INTO_CANONICAL
KEEP_REFERENCE_SOURCE
ARCHIVE_HISTORY
DELETE_ABSORBED_HISTORY
REVIEW_SEMANTIC_DUPLICATE
```

A binary document does not override a higher-priority current-state Markdown
baseline merely because it is stored in this directory.

## Maintenance rule

New architecture decisions should update an existing canonical document where
possible instead of creating a new phase-specific document.

Phase-specific audit evidence is acceptable during controlled consolidation,
but it must not become the permanent navigation model.
