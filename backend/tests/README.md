# SIXPAY CONNECT — Test Foundation

## 1. Purpose

This module defines the common backend testing foundation for SIXPAY CONNECT
and hosts tests that require multiple bounded contexts or the assembled
application.

The repository follows the testing principle:

```text
Module-owned behavior
        ↓
tested inside the owning module

Cross-module / assembled behavior
        ↓
tested inside backend/tests
```

## Responsibilities

This module owns:

- assembled Spring application-context verification;
- contract-backed cross-module integration scenarios;
- full-stack persistence and hybrid-security integration tests;
- permanent repository-level golden coverage gates.

It does not own module-local domain, application, API or persistence tests.
Those remain beside the implementation in the owning module. The `partner`
module remains the golden business-module reference.

## Canonical references

- `TEST-MATRIX.md` — test categories and execution ownership;
- `CROSS-MODULE-GATE.md` — cross-module closure criteria;
- `BACKEND-GOLDEN-COVERAGE-GATE.md` — module coverage evidence gate.

## Execution

From `backend/`:

```bash
mvn -pl tests test
mvn -pl tests -Pfull-tests verify
```

Repository-wide verification remains:

```bash
python scripts/verify_baseline.py
```
