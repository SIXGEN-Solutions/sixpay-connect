# FS-2.7.5 — AI Documentation Consolidation

## Objective

Normalize the role of `documentation/ai/` so AI working artifacts cannot become
a competing repository baseline.

## Findings

The AI tree contains three main areas:

```text
customer/
integration/
payment/
```

Customer contains AI context manifests, generation briefs, preflight documents
and blocking-decision assets.

Integration contains a large set of lot-oriented implementation notes.

Payment contains manifests, preflight documents, decisions, reviews, acceptance
scenarios, plans, gap analyses and historical lot documents.

## Key issue

The content is valuable for traceability, but filenames such as:

```text
*_LOT_*
GATE_*
*_PLAN*
*_GAP_ANALYSIS*
```

describe delivery history rather than the current repository state.

Without an explicit policy, an AI assistant could incorrectly treat an old lot
document as authoritative.

## Decisions

```text
documentation/ai/README.md
    -> KEEP_CANONICAL navigation/policy

AI_CONTEXT_MANIFEST.yaml
    -> KEEP_REFERENCE_SOURCE

AI files explicitly referenced by CONTRACT_REGISTRY.yaml
    -> KEEP_REFERENCE_SOURCE

acceptance scenarios and validated decision/review material
    -> KEEP_REFERENCE_SOURCE

lot/preflight/plan/gap-analysis working documents
    -> REVIEW_HISTORY in FS-2.7.6
```

## Precedence

AI documentation remains below:

```text
implementation
architecture
requirements
contracts
```

in repository source-of-truth precedence.

## Golden module

Any AI-generated business-module structure must remain aligned with:

```text
backend/partner
```

unless an explicit architecture decision says otherwise.

## No deletion

FS-2.7.5 changes navigation and policy only.

Historical AI artifacts are reviewed for deletion/archive in FS-2.7.6 after
reference analysis.
