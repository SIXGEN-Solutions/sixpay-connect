# SIXPAY CONNECT — Canonical Documentation Map

This file is the canonical table of contents for SIXPAY CONNECT documentation.

It does **not** replace the source-of-truth order defined in
`ENGINEERING_CONTEXT.md`. It tells engineers and AI assistants where to navigate
for each kind of information and which specialized index is authoritative.

## Source-of-truth precedence

When sources conflict, use the precedence defined by `ENGINEERING_CONTEXT.md`:

1. authoritative implementation branch;
2. `documentation/architecture/`;
3. `documentation/requirements/`;
4. `documentation/contracts/`;
5. `documentation/ai/`;
6. engineering assets;
7. `ENGINEERING_CONTEXT.md`.

The documentation map is a navigation layer, not a competing authority.

## Canonical documentation map

| Need | Canonical location | Role |
|---|---|---|
| System and application architecture | `documentation/architecture/README.md` | Canonical architecture map and current-state navigation |
| Integration landscape | `documentation/architecture/integration/` | Producer/consumer landscape and integration ownership |
| Internal architecture | `documentation/architecture/internal/` | Internal application/runtime architecture |
| Module boundaries | `documentation/architecture/MODULE_BOUNDARIES.md` | Canonical cross-module boundary policy |
| Configuration architecture | `documentation/architecture/configuration/` | Runtime configuration ownership, profiles, auth, Springdoc, Angular environments and feature flags |
| Repository hygiene | `documentation/architecture/REPOSITORY_HYGIENE.md` | Tracked-artifact retention and cleanup policy |
| Business requirements | `documentation/requirements/README.md` | Canonical requirements navigation and source policy |
| CDC / source requirement documents | `documentation/requirements/cdc/` | Source business requirement material |
| User stories | `documentation/requirements/user-stories/` | User-story level requirements |
| API/event/integration contracts | `documentation/contracts/` | Physical contractual interfaces |
| Contract classification and lifecycle | `documentation/contracts/CONTRACT_REGISTRY.yaml` | Canonical contractual table of contents |
| Contract navigation and policy | `documentation/contracts/README.md` | Contract structure and registry semantics |
| Domain-focused documentation | `documentation/domains/README.md` | Canonical domain-documentation policy and current-state navigation |
| Implementation guidance | `documentation/implementation/` | Implementation-oriented documentation |
| Runbooks | `documentation/runbooks/README.md` | Canonical operational navigation and contract-reference policy |
| Onboarding | `documentation/onboarding/` | Engineer onboarding documentation |
| AI engineering assets | `documentation/ai/README.md` | AI navigation, precedence and historical-working-asset policy |
| Stubs/reference fixtures | `documentation/stubs/` | Non-canonical reference/stub material |

## Contractual special rule

`documentation/contracts/CONTRACT_REGISTRY.yaml` is the canonical contractual
table of contents.

Physical OpenAPI/JSON Schema/event contracts describe interfaces.

The registry describes their:

- classification;
- capability;
- ownership;
- direction;
- source system;
- system of record;
- lifecycle;
- approval;
- generation policy;
- security;
- MVP usage.

The documentation map must not duplicate the registry at contract-entry level.

## Architecture special rule

`documentation/architecture/` has higher precedence than requirements,
contracts and AI documentation when architectural sources conflict, as defined
by `ENGINEERING_CONTEXT.md`.

Architecture documentation should describe the **current baseline**, not the
history of how the baseline was built.

FS-2.x inventory/audit documents are transitional consolidation evidence until
FS-2.7 decides whether each one remains canonical, is merged, archived or
deleted.

## Binary source documents

Binary files such as `.docx` or `.pdf` are not automatically canonical solely
because they live under `documentation/architecture/`.

They must receive an explicit FS-2.7 decision:

```text
KEEP_CANONICAL
MERGE_INTO_CANONICAL
KEEP_REFERENCE_SOURCE
ARCHIVE_HISTORY
DELETE_ABSORBED_HISTORY
REVIEW_SEMANTIC_DUPLICATE
```

Until reviewed, a binary document must not silently override a higher-priority
Markdown/YAML baseline.

## Domain implementation reference

For business-module structure and implementation conventions:

```text
backend/partner
```

remains the golden module.

Documentation describing another domain must not create a structural convention
that contradicts the golden Partner module without an explicit architecture
decision.

## Developer / AI navigation

Start here:

```text
ENGINEERING_CONTEXT.md
        ↓
documentation/README.md
        ↓
specialized canonical location
        ↓
implementation / contracts / tests
```

For contracts:

```text
ENGINEERING_CONTEXT.md
        ↓
documentation/README.md
        ↓
documentation/contracts/README.md
        ↓
CONTRACT_REGISTRY.yaml
        ↓
physical contract
```

## Documentation maintenance rule

A new document must have:

- a clear owner or owning concern;
- one canonical location;
- a reason it does not duplicate an existing canonical document;
- references updated when it supersedes another document.

Temporary change artifacts must not become permanent baseline documentation.

## FS-2.7 status

This index is established by:

```text
FS-2.7.1 — Canonical documentation map/index
```

Subsequent FS-2.7 work may merge, archive or delete documents, but this file
remains the stable navigation entry point for `documentation/`.

## Documentation verification

Canonical documentation non-regression gate:

```bash
py scripts/verify_documentation_baseline.py
```

This gate protects the canonical documentation topology, absorbed historical
cleanup, contracts/runbooks references and AI documentation precedence.

## Final documentation validation

To validate the complete consolidated documentation baseline:

```bash
py scripts/verify_documentation_final.py
```

This is the canonical FS-2.7 closure command. It composes the documentation
non-regression gate, contract-registry integrity and configuration-documentation
alignment without reimplementing their specialized rules.
