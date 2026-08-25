# FS-2.7.4 — Contracts / Runbooks References Alignment

## Objective

Ensure that operational documentation references the canonical contractual
baseline rather than historical filenames or transitional artifacts.

## Existing baseline

The contract area already owns:

```text
documentation/contracts/README.md
documentation/contracts/CONTRACT_REGISTRY.yaml
```

The registry is the canonical contractual table of contents.

The runbook area is organized by operational concern:

```text
accounting
amplitude
integration
kafka
notification
payment
tresorpay
```

## Decisions

```text
contracts/README.md
    -> KEEP_CANONICAL

contracts/CONTRACT_REGISTRY.yaml
    -> KEEP_CANONICAL

runbooks/README.md
    -> KEEP_CANONICAL

specialized runbooks
    -> KEEP, subject to reference-integrity gate
```

## Alignment rule

A contract-like path referenced by a runbook must:

```text
exist on filesystem
AND
be registered when it is a canonical physical contract
AND
not be historical/transitional
```

## Historical names

The gate rejects known absorbed/removed artifacts, including:

```text
CONTRACT_REGISTRY_LOT0_PATCH.md
payment-query-api-v1-status-alignment.patch
administration-query-api-v1.yaml
incident-query-api-v1.yaml
```

## Non-duplication rule

Runbooks can document operational procedures, validation steps, expected
responses and troubleshooting.

They must not become a second contract registry or reproduce an entire OpenAPI
contract as an independently maintained source.

## Outcome

FS-2.7.4 introduces a permanent documentation reference verifier without
changing any runtime behavior or contract semantics.
