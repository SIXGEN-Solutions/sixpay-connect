# SIXPAY CONNECT — AI Documentation

This directory contains AI-oriented engineering assets: manifests, preflight
documents, generation briefs, implementation plans, gap analyses, acceptance
scenarios and historical lot-by-lot working notes.

## Canonicality

AI documentation is **not** an independent source of truth.

Repository precedence remains:

```text
authoritative implementation branch
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

AI assets may:

```text
guide generation
record preflight decisions
capture implementation intent
record gap analysis
provide acceptance scenarios
preserve historical traceability
assemble domain-specific context
```

They must not silently redefine:

```text
architecture
approved contracts
runtime configuration
module ownership
database ownership
security policy
business requirements
```

## Current AI areas

### Customer

`documentation/ai/customer/` contains:

```text
AI_CONTEXT_MANIFEST.yaml
CUSTOMER_DOMAIN_GENERATION_BRIEF.md
GATE_IA_0_CUSTOMER_PREFLIGHT.md
IA_0R_BLOCKING_DECISIONS.md
IA_0R_BLOCKING_DECISIONS.yaml
```

Classification:

```text
AI_CONTEXT_MANIFEST.yaml
    -> KEEP_REFERENCE_SOURCE

generation brief / preflight / blocking decisions
    -> KEEP_REFERENCE_SOURCE pending FS-2.7.6 historical cleanup review
```

### Integration

`documentation/ai/integration/` contains many lot-oriented implementation notes,
including Accounting, Notification, Payment/Amplitude and integration foundation
lots.

Classification:

```text
*_LOT_*.md
    -> HISTORICAL_AI_WORKING_ASSET

other integration AI notes
    -> REVIEW_REFERENCE_SOURCE
```

These files are useful for traceability but should not be the primary navigation
surface for the current repository baseline.

### Payment

`documentation/ai/payment/` contains:

```text
AI_CONTEXT_MANIFEST.yaml
preflight documents
implementation manifests/plans
acceptance scenarios
aggregate/domain notes
contract gap analyses
review/decision documents
historical lot documents
```

Classification:

```text
AI_CONTEXT_MANIFEST.yaml
    -> KEEP_REFERENCE_SOURCE

validated decision/review documents referenced by canonical contracts
    -> KEEP_REFERENCE_SOURCE

acceptance scenarios
    -> KEEP_REFERENCE_SOURCE

implementation plans / lot documents / gap analyses
    -> HISTORICAL_AI_WORKING_ASSET unless still actively referenced
```

## Contract-reference exception

Some AI review documents are explicitly referenced from
`documentation/contracts/CONTRACT_REGISTRY.yaml`.

Those files are retained as traceability/reference sources while those registry
references exist.

They still do not override the physical contract or registry semantics.

## AI navigation rule

AI assistants should start with:

```text
ENGINEERING_CONTEXT.md
        ↓
documentation/README.md
        ↓
canonical architecture / requirements / contracts
        ↓
documentation/ai/README.md
        ↓
domain-specific AI reference only when needed
```

Do not start implementation from a historical `LOT_*`, `GATE_*`, `PLAN_*` or
`GAP_ANALYSIS_*` file without first validating it against the current branch and
higher-priority documentation.

## Golden module

For any AI-assisted generation or modification of a business module:

```text
backend/partner
```

## Generation rule

Before generating or modifying code from AI documentation:

```text
1. inspect authoritative implementation;
2. inspect golden Partner module for structural conventions;
3. inspect architecture;
4. inspect requirements;
5. inspect contract registry and physical contracts;
6. use AI documents only as supporting context.
```

## Historical cleanup

FS-2.7.5 does not delete AI assets.

FS-2.7.6 may later classify and remove/archival candidates such as:

```text
*_LOT_*.md
*_PLAN.md
*_GAP_ANALYSIS.md
GATE_*.md
preflight-only working documents
```

but only after verifying that no canonical registry, architecture, requirement,
runbook or implementation asset still relies on them.
