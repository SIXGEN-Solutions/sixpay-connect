# FS-2.5.5 — OpenAPI / Springdoc Configuration Consolidation

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.5 — Configuration consolidation`  
**Golden module:** Partner

## Purpose

FS-2.5.5 formalizes the separation between:

```text
Bootstrap
    = runtime Springdoc/OpenAPI assembly and grouping

Business module
    = API implementation, endpoint ownership and module OpenAPI resources

documentation/contracts
    = canonical interface contracts and contractual registry
```

No endpoint, route, group name or contract is changed by this phase.

## Bootstrap-owned runtime configuration

Bootstrap owns:

```text
springdoc.api-docs.*
springdoc.swagger-ui.*
OpenApiConfiguration
GroupedOpenApi beans
global API metadata
global bearerAuth security scheme
```

The base runtime keeps Springdoc disabled:

```text
springdoc.api-docs.enabled = false
springdoc.swagger-ui.enabled = false
```

The existing standalone developer profile keeps it enabled for local
inspection.

## Canonical OpenAPI groups

The current assembled runtime groups are:

```text
partner
customer
payment
administration
reporting
accounting
```

Their existence and names are part of the consolidated runtime baseline.

## Important routing ownership

The Payment timeline endpoint:

```text
/internal/api/v1/payments/{paymentId}/timeline
```

is intentionally:

```text
excluded from Payment
included in Reporting
```

This preserves the earlier architectural decision that the endpoint is exposed
under the Payment-shaped URI while belonging to the Reporting/Payment Audit
capability.

## Administration grouping

Administration intentionally groups:

```text
/internal/api/v1/administration/users/**
/internal/api/v1/administration/overview
/internal/api/v1/administration/settings
/internal/api/v1/administration/integrations
/internal/api/v1/incidents/**
```

This is consistent with the consolidated Administration operational contract
and Security user-administration exposure.

## Domain ownership

A business module may own:

- controllers;
- endpoint annotations;
- DTO/API model;
- module-specific OpenAPI resources;
- module-specific contractual metadata.

A business module must not define modular-monolith `GroupedOpenApi` runtime
assembly.

The Partner golden module already follows this pattern by owning an `openapi/`
resource directory while Bootstrap performs global grouping.

## Canonical contracts versus runtime docs

Springdoc runtime output is not the contractual source of truth.

```text
documentation/contracts/CONTRACT_REGISTRY.yaml
        +
canonical physical contracts
        = contractual truth

Springdoc runtime
        = assembled implementation documentation
```

Any divergence remains a contract-conformance issue and must be handled by the
existing contract gates.

## Non-regression policy

FS-2.5.5 does not:

- rename API groups;
- move endpoints between groups except through a reviewed architecture change;
- rename endpoint paths;
- change Springdoc availability by environment;
- change JWT security semantics;
- generate code from runtime Springdoc output;
- merge domain contracts into one monolithic OpenAPI file.

## Gate rules

The gate protects:

1. Bootstrap ownership of `OpenApiConfiguration`.
2. Exactly the reviewed six `GroupedOpenApi` group identifiers.
3. Payment timeline exclusion from `payment`.
4. Payment timeline inclusion in `reporting`.
5. Administration incident/operational paths.
6. base Springdoc disabled-by-default semantics.
7. standalone Springdoc enabled semantics.
8. absence of `GroupedOpenApi` definitions from business modules.

## Exit criteria

FS-2.5.5 is DONE when the ownership model is documented and the runtime
OpenAPI/Springdoc gate is green without functional API changes.
