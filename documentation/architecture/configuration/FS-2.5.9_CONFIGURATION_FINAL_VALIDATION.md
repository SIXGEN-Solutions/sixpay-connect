# FS-2.5.9 — Configuration Consolidation Final Validation

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.5 — Configuration consolidation`  
**Golden module:** Partner

## Purpose

FS-2.5.9 is the exit gate for the complete configuration-consolidation phase.

It adds no new configuration rule. It proves that the rules established from
FS-2.5.0 through FS-2.5.8 coexist with the complete backend and frontend
baseline.

## Final validation chain

```text
FS-2.5.8 configuration non-regression gate
        ↓
backend full Maven reactor verify
        ↓
frontend unit tests
        ↓
frontend build:all
        ↓
FS-2.5 FINAL VALIDATION PASSED
```

## What `build:all` already proves

The canonical frontend `build:all` pipeline executes the environment gate and
the integration build gates before producing all reviewed build variants.

It covers:

```text
Angular environment policy
contract consolidation
runtime datasource policy
full-stack static conformance
integration contract-backed policy
integration build
Netlify/demo build
production build
```

Therefore FS-2.5.9 does not duplicate those commands individually.

## Backend proof

`mvn verify` is mandatory after the targeted FS-2.5 architecture gates.

This validates the complete Maven reactor rather than only Bootstrap's
configuration architecture tests.

## Regression policy

A final-validation failure does not authorize broad cleanup or semantic
rewrites.

The workflow remains:

```text
identify failing gate
  -> inspect concrete regression
  -> minimal correction
  -> rerun targeted check
  -> rerun FS-2.5.9
```

## Exit criteria

FS-2.5 can be closed only when all of the following are green:

- FS-2.5.8 consolidated configuration gate;
- full backend `mvn verify`;
- frontend unit tests;
- frontend `build:all`;
- no missing FS-2.5 documentation or gate asset.

## Closure statement

When the script prints:

```text
FS-2.5.9 FINAL VALIDATION PASSED
```

the configuration-consolidation phase can be marked:

```text
FS-2.5 — Configuration consolidation
STATUS: CLOSED
```
