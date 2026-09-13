# SIXPAY CONNECT — Architecture Documentation

This directory is the canonical architecture source for SIXPAY CONNECT,
subject to the source-of-truth precedence defined in `ENGINEERING_CONTEXT.md`.

Architecture documentation describes the **current repository baseline**.
Implementation-phase or delivery evidence may be retained only when explicitly
classified as historical material; it is not part of the canonical navigation
surface.

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
| Accounting T1 architecture | `documentation/architecture/accounting/ACCOUNTING_T1_ARCHITECTURE.md` |

## Golden business-module reference

`backend/partner` remains the structural and implementation reference for
business modules.

## Historical material

Historical implementation and audit documents do not define current
architecture. Their durable conclusions must be reflected in canonical
current-state documents and permanent verification rules.

Git history remains the source for reconstructing superseded consolidation
steps when needed.

## Maintenance rule

New architecture decisions should update an existing canonical document where
possible instead of creating phase-specific documents.

Temporary audit evidence must not become the permanent navigation model.
