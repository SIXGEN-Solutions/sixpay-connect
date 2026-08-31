# SIXPAY CONNECT — Domain Documentation

This directory is reserved for **validated current-state domain documentation**.

## Current state

The directory does not yet contain substantial canonical domain documentation.
FS-2.9 removes the former empty `customer/` placeholder so the tracked tree
describes only implemented or documented repository content.

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

## FS-2.9 hygiene decision

Current tracked domain tree includes validated current-state Payment policy
documentation:

```text
documentation/domains/
├── README.md
└── payment/
    └── PAYMENT_POLICY_BASELINE.md
```

`payment/PAYMENT_POLICY_BASELINE.md` is the canonical `payment-mvp/v1`
business-policy reference used to align Payment runtime policy and future Core
Banking API implementation.

Empty domain placeholders remain forbidden. A domain directory is created only
when validated current-state domain documentation is explicitly produced.
