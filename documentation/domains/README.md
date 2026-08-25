# SIXPAY CONNECT — Domain Documentation

This directory is reserved for **validated current-state domain documentation**.

## Current state

At FS-2.7.3, the directory does not yet contain substantial canonical domain
documentation.

The existing `documentation/domains/customer/` directory contains only a
`.gitkeep` placeholder.

Therefore FS-2.7.3 does **not** infer or generate business-domain documentation
from implementation classes, requirements or AI briefs.

## Domain documentation rule

A domain document belongs here only when it describes validated current-state
business/domain knowledge that is useful independently from:

```text
raw requirements
architecture decisions
physical contracts
implementation source code
AI working notes
```

Examples of suitable future content:

```text
domain vocabulary
aggregates and invariants
business state transitions
business ownership
domain events and meaning
business policies
```

## Source precedence

Domain documentation must remain consistent with:

```text
authoritative implementation branch
documentation/architecture/
documentation/requirements/
documentation/contracts/
```

If a domain document conflicts with the implementation or an approved
architecture/contract source, the higher-priority source wins.

## Golden implementation reference

`backend/partner` remains the golden business-module implementation and folder
structure reference.

That structural convention must not be inferred from an incomplete domain
document.

## No duplication rule

Do not copy raw CDC/user-story content into a domain README simply to make the
domain tree look complete.

A domain document should exist only when there is validated, durable domain
knowledge worth maintaining as an independent current-state artifact.

## FS-2.7.3 decision

Current domain tree:

```text
documentation/domains/
└── customer/
    └── .gitkeep
```

Decision:

```text
KEEP_PLACEHOLDER
```

until validated domain documentation is explicitly produced.
