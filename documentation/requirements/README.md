# SIXPAY CONNECT — Requirements Documentation

This directory is the canonical navigation entry point for business and
functional requirement sources.

## Role

Requirement documents describe business intent, functional expectations,
constraints and source material. They do not override the authoritative
implementation revision, architecture decisions or canonical contracts.

## Current source map

`documentation/requirements/cdc/` contains retained CDC and functional
specification source documents. `documentation/requirements/user-stories/`
contains retained user-story source material.

Their presence does not mean every statement remains implemented or
contractually approved.

## Canonicality rule

```text
requirements  -> business intent / functional source
architecture  -> current technical/architectural decisions
contracts     -> approved interface definition and lifecycle
implementation-> actual behavior on authoritative revision
```

## Binary document rule

PDF/DOCX files are legitimate requirement-source documents. Their authority and
retention follow explicit documentation classification.

## Maintenance

New requirements should be added under the most specific requirements subtree.
Domain documentation is for validated current-state domain knowledge, not raw
requirement intake.
