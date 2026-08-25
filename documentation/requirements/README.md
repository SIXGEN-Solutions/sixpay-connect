# SIXPAY CONNECT — Requirements Documentation

This directory is the canonical navigation entry point for business and
functional requirement sources.

## Role

Requirement documents describe business intent, functional expectations,
constraints and source material received or produced during product definition.

They do not override:

```text
1. the authoritative implementation branch;
2. architecture decisions in documentation/architecture/;
3. canonical contracts in documentation/contracts/.
```

When a requirement conflicts with a higher-priority source, follow the
repository source-of-truth order defined in `ENGINEERING_CONTEXT.md`.

## Current source map

### CDC / functional specification sources

`documentation/requirements/cdc/` currently contains:

```text
Cahier des Charges interopérabilité entre TRESOR PAY et le core banking...
SIXPAY_CONNECT_CDC.pdf
SIXPAY_CONNECT_Specifications_Fonctionnelles.pdf
```

These files are retained as **requirement source documents**.

Their presence in this directory does not mean that every statement they
contain is still implemented or contractually approved.

Implementation and contract conformance must be checked against the
authoritative branch and the contract registry.

### User stories

`documentation/requirements/user-stories/` currently contains:

```text
SIXPAY_CONNECT_USER_STORIES.docx
```

This file is retained as a requirement source.

## Canonicality rule

Requirement source documents are canonical for the requirement material they
contain, but not for architecture, implementation shape or contractual runtime
interfaces.

Use:

```text
requirements
    -> business intent / functional source

architecture
    -> current technical/architectural decisions

contracts
    -> approved interface definition and lifecycle

implementation
    -> actual behavior on authoritative branch
```

## Binary document rule

PDF/DOCX requirement files are legitimate source documents and are not deleted
merely because they are binary.

They may later receive one FS-2.7 decision:

```text
KEEP_REFERENCE_SOURCE
MERGE_INTO_CANONICAL
ARCHIVE_HISTORY
DELETE_ABSORBED_HISTORY
REVIEW_SEMANTIC_DUPLICATE
```

Deletion requires proof that their useful requirement content is fully absorbed
elsewhere.

## Maintenance

New requirements should be added under the most specific requirements subtree
and should not be placed in `documentation/domains/` merely to create a domain
summary.

Domain documentation is for validated current-state domain knowledge, not raw
requirement intake.
