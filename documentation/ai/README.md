# SIXPAY CONNECT — AI Documentation

This directory contains AI-oriented engineering assets used to support analysis,
generation, review and traceability.

## Canonicality

AI documentation is **not** an independent source of truth.

Repository precedence remains:

```text
authoritative implementation revision
        ↓
documentation/architecture/
        ↓
documentation/requirements/
        ↓
documentation/contracts/
        ↓
documentation/ai/
        ↓
engineering assets
        ↓
ENGINEERING_CONTEXT.md
```

When an AI document conflicts with a higher-priority source, the higher-priority
source wins.

## Purpose

AI assets may support context assembly, generation briefs, implementation
intent, gap analysis, acceptance scenarios, validated decision traceability and
domain-specific engineering context.

They must not silently redefine architecture, approved contracts, runtime
configuration, module ownership, database ownership, security policy or business
requirements.

## Current AI areas

### Customer

Customer AI assets include a domain context manifest and retained supporting
engineering references. They are loaded only when the Customer capability
requires them and only after higher-authority current-state sources.

### Payment

Payment AI assets include a domain context manifest, retained decision evidence,
acceptance scenarios and historical implementation working material.

Only specifically required non-historical references may be loaded as active
supporting context. Historical working material is retained for traceability and
must not drive current implementation.

### Accounting

`documentation/ai/accounting/ACCOUNTING_T1_AI_CONTEXT.md` is the consolidated
Accounting supporting reference. It summarizes the implemented candidate,
cutoff, batch, approved Core Banking Accounting submission and TFJ finality
baseline after higher-authority sources are loaded.

It never overrides implementation, architecture, requirements or registered
physical contracts.

### Integration

Integration AI assets retain supporting and historical material for external
integration work. Current implementation must always be derived from the owning
domain, integration architecture and registered physical contracts.

## Historical material

Historical AI working assets are explicitly classified by
`documentation/DOCUMENTATION_CLASSIFICATION.yaml` and excluded from active
Master Prompt context.

Historical implementation working assets use the repository classification
`HISTORICAL_AI_WORKING_ASSET`. This is a durable classification label, not a
delivery phase or implementation-step identifier.

Their purpose is traceability only. Durable conclusions must be absorbed into
current architecture, requirements, contracts, implementation, tests or
canonical documentation. Git history remains the preferred source for
reconstructing delivery chronology.

## Contract-reference exception

Some AI review documents may be referenced by
`documentation/contracts/CONTRACT_REGISTRY.yaml` as supporting evidence.

Such references do not make the AI document authoritative over the registry or
physical contract.

## AI navigation rule

AI-assisted work starts with:

```text
ENGINEERING_CONTEXT.md
        ↓
MASTER_PROMPT_INPUT_MANIFEST.yaml
        ↓
canonical architecture / requirements / contracts
        ↓
authoritative implementation
        ↓
documentation/ai/README.md
        ↓
specific supporting AI reference only when required
```

Do not start implementation from historical working material without validating
the requirement against the current authoritative revision and all
higher-priority sources.

## Golden module

For AI-assisted generation or modification of a business module, use
`backend/partner` as the structural and implementation-quality reference. Do not
copy Partner business rules into another domain.

## Generation rule

Before generating or modifying code from AI documentation:

1. inspect the authoritative implementation;
2. inspect `backend/partner` for structural conventions;
3. inspect architecture;
4. inspect applicable requirements;
5. resolve contracts through `CONTRACT_REGISTRY.yaml`;
6. verify lifecycle, approval, generation policy and generation permission;
7. use AI material only as supporting context.

## Formal documentation classification

The repository-wide classification is maintained in
`documentation/DOCUMENTATION_CLASSIFICATION.yaml`.

| Classification | Meaning | Active-context policy |
|---|---|---|
| `CANONICAL` | Current architecture, approved requirements, active contracts and stable verification rules | Active according to source precedence |
| `REFERENCE_SOURCE` | Supporting evidence and external source material | Load only when needed |
| `HISTORICAL` | Superseded implementation/delivery working material | Retained for traceability; excluded from active context |
| `TEMPLATE` | Reusable generation models | Used only for an applicable generation workflow |

The active historical-document exclusion list is maintained by
`MASTER_PROMPT_INPUT_MANIFEST.yaml`.

## Active Master Prompt input

`MASTER_PROMPT_INPUT_MANIFEST.yaml` is the deterministic selector for active AI
context. It defines mandatory current-state sources, on-demand sources,
historical exclusions, contract selection and generation safety constraints.

Validate it with:

```bash
py scripts/verify_master_prompt_input_manifest.py
```

The manifest selects context but never replaces source authority, approval state
or generation policy.

## Active Master Engineering Prompt

`MASTER_ENGINEERING_PROMPT.md` is the single active repository-wide engineering
instruction for AI-assisted work.

Superseded instructions belong to Git history and must not be combined with the
active prompt.

Validate it with:

```bash
py scripts/verify_master_engineering_prompt.py
```

## Maintenance rule

AI navigation and current-state summaries must remain independent from delivery
chronology. When a temporary implementation artifact no longer contributes
unique current-state knowledge, preserve its history through Git rather than
promoting it into canonical navigation.
