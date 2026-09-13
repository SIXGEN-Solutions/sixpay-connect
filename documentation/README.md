# SIXPAY CONNECT — Canonical Documentation Map

This file is the canonical table of contents for SIXPAY CONNECT documentation.

It does **not** replace the source-of-truth order defined in
`ENGINEERING_CONTEXT.md`. It tells engineers and AI assistants where to navigate
for each kind of information and which specialized index is authoritative.

## Source-of-truth precedence

When sources conflict, use the precedence defined by `ENGINEERING_CONTEXT.md`:

1. authoritative implementation revision;
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
| Active Master Prompt input | `MASTER_PROMPT_INPUT_MANIFEST.yaml` | Deterministic active-source selection, exclusions and readiness evidence |
| Active Master Engineering Prompt | `MASTER_ENGINEERING_PROMPT.md` | Canonical AI engineering orchestration for repository changes |
| Stubs/reference fixtures | `documentation/stubs/` | Non-canonical reference/stub material |

## Formal documentation classification

The complete classification is maintained in
`documentation/DOCUMENTATION_CLASSIFICATION.yaml`.

The repository categories are:

- `CANONICAL`: current architecture, approved requirements, active contracts
  and stable navigation/verification rules;
- `REFERENCE_SOURCE`: external PDF/DOCX source material retained for traceability;
- `HISTORICAL`: implementation-phase and delivery documents retained without
  active authority;
- `TEMPLATE`: reusable models for future generation work.

Historical implementation documents are excluded from the active Master Prompt
by the classification index and must not be used as current-state authority.

## Contractual special rule

`documentation/contracts/CONTRACT_REGISTRY.yaml` is the canonical contractual
table of contents. Physical OpenAPI/JSON Schema/event contracts describe
interfaces; the registry describes classification, capability, ownership,
direction, lifecycle, approval, generation policy, security and MVP usage.

## Architecture special rule

Architecture documentation describes the **current baseline**, not the history
of how the baseline was built. Intermediate audit, phase and delivery evidence
belongs to Git history or explicitly classified `HISTORICAL` material.

## Binary source documents

Binary files such as `.docx` or `.pdf` are not automatically canonical solely
because of their directory. Their authority comes from explicit classification
and source role. Reference-source documents do not silently override
higher-priority current-state Markdown/YAML baselines.

## Domain implementation reference

`backend/partner` remains the golden business-module implementation reference.

## Developer / AI navigation

```text
ENGINEERING_CONTEXT.md
        ↓
documentation/README.md
        ↓
specialized canonical location
        ↓
implementation / contracts / tests
```

## Documentation maintenance rule

A new document must have a clear owner, one canonical location, a reason it does
not duplicate an existing canonical document, and updated references when it
supersedes another document.

Temporary change artifacts must not become permanent baseline documentation.

## Documentation verification

```bash
py scripts/verify_documentation_baseline.py
py scripts/verify_documentation_final.py
```
