# SIXPAY CONNECT — Repository Hygiene

## Purpose

This document defines the canonical repository-hygiene policy. The authoritative
revision must describe the current system, not local build output, empty future
structure or superseded delivery scaffolding.

## Retention rules

Tracked content must have a current repository purpose. Retained content includes
implementation, tests, executable validation gates, canonical architecture,
requirements and contracts, approved reference sources, historical assets that
are explicitly classified, active developer configuration and required runtime
contract mirrors.

`backend/partner` remains the golden business-module reference.

## Forbidden tracked artifacts

The canonical tree must not contain build output, generated reports, dependency
directories, temporary patch/apply scripts, editor backups, compiled binaries,
zero-byte placeholders, empty future modules or duplicate source/reference
documents without an explicit reason.

Git history is the preservation mechanism for removed delivery evidence.
Generated delivery patches must remain outside the repository.

## Ownership decisions

- Customer enrollment and `CustomerSubscription` lifecycle behavior remain in
  `backend/customer`.
- There is no autonomous `backend/subscription` bounded context.
- Canonical contracts live under `documentation/contracts`.
- Empty future deployment/tooling placeholders are not retained.
- Generated Playwright reports are CI artifacts and are ignored by Git.

## Verification

```bash
python scripts/verify_repository_hygiene.py
python scripts/verify_baseline.py
```
