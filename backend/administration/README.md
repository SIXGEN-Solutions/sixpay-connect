# Administration

## Current repository status

`administration` is declared as a SIXPAY CONNECT business-domain Maven module,
but the authoritative Phase 8 branch does not currently expose a materialized
Administration implementation comparable to the golden `partner` module.

The current module POM declares only:

```text
com.sixpay:common
```

No Administration-owned Spring MVC, validation, persistence, security,
OpenAPI or test infrastructure is currently declared.

## Phase 8 — Backend Golden Test Coverage

Golden reference:

```text
backend/partner
```

Current Administration classification:

```text
Domain          NOT_IMPLEMENTED
Application     NOT_IMPLEMENTED
API             NOT_IMPLEMENTED
Infrastructure  NOT_IMPLEMENTED
```

This is not a test failure. It is an implementation-state classification.

Phase 8.2.7 MUST NOT invent placeholder production classes or synthetic tests
solely to make the module look structurally identical to `partner`.

## Test policy

When Administration implementation is introduced, it must follow the same
layered testing discipline as the golden module:

```text
src/test/java/com/sixpay/administration/
├── api/
├── application/
│   └── service/
├── domain/
└── infrastructure/
    └── persistence/
```

Only layers that actually exist shall have tests.

Cross-module Administration scenarios belong in:

```text
backend/tests/
```

and not in this module.

## Validation

The current Maven shell can still be validated from `backend/`:

```bash
mvn --batch-mode --no-transfer-progress     -pl administration -am test
```

and:

```bash
mvn --batch-mode --no-transfer-progress     -pl administration -am clean verify
```

Detailed Phase 8.2.7 evidence is maintained in:

```text
ADMINISTRATION-TEST-COVERAGE.md
```
