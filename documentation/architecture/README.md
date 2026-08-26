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
| Repository hygiene | `documentation/architecture/REPOSITORY_HYGIENE.md` |
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

## Historical consolidation evidence

Phase-oriented FS-2.4, FS-2.5 and FS-2.6 architecture documents have been
removed after their durable conclusions were absorbed into the canonical
current-state documents and permanent verification gates.

FS-2.7 phase reports are likewise removed once their policies are represented
by the canonical documentation indexes.

Git history remains the source for reconstructing those consolidation steps.

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
