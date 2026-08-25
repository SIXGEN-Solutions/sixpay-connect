# SIXPAY CONNECT — Runbooks

This directory contains operational, release, integration and support procedures.

## Canonical contract rule

Runbooks may explain **how to operate or validate** a capability, but they do
not define contractual interfaces.

For any API/event/schema reference:

```text
documentation/contracts/README.md
        ↓
documentation/contracts/CONTRACT_REGISTRY.yaml
        ↓
canonical physical contract
```

The physical contract defines the interface.

`CONTRACT_REGISTRY.yaml` defines its classification, ownership, lifecycle,
approval, generation policy, security and MVP usage.

## Current runbook areas

```text
accounting/
amplitude/
integration/
kafka/
notification/
payment/
tresorpay/
```

Each directory owns procedures for its operational concern.

## Reference policy

A runbook reference to a contractual artifact must satisfy all of the following:

1. the referenced file exists under `documentation/contracts/`;
2. the referenced physical contract is registered in
   `documentation/contracts/CONTRACT_REGISTRY.yaml`;
3. the reference does not target a historical patch, backup or removed contract;
4. the runbook does not redefine the contract inline as a competing source of
   truth.

## Forbidden references

Historical/transitional artifacts are forbidden, including:

```text
*.patch
*_PATCH.md
CONTRACT_REGISTRY_LOT0_PATCH.md
administration-query-api-v1.yaml
incident-query-api-v1.yaml
payment-query-api-v1-status-alignment.patch
```

Administration and Incident operational queries are represented by the
consolidated physical contract defined in the canonical registry.

## Operational documentation precedence

Runbooks are lower-level operational guidance.

When a runbook conflicts with:

```text
authoritative implementation branch
architecture documentation
requirements
canonical contracts
```

the higher-priority source wins according to `ENGINEERING_CONTEXT.md`.

## Maintenance

When a contract is renamed, consolidated or removed:

```text
registry
    -> update first

physical contract
    -> update consistently

runbook references
    -> align in the same change

FS-2.7.4 gate
    -> must remain green
```
