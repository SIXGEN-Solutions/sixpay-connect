# SIXPAY CONNECT — Configuration Architecture

## Status

Canonical current-state configuration architecture.

This document absorbs the stable conclusions of FS-2.5.1 through FS-2.5.8.
Detailed FS-2.5 documents remain temporary consolidation evidence.

## Ownership model

Configuration has two semantic ownership levels:

```text
Bootstrap
    = runtime/global composition

Business module
    = domain-owned semantics/defaults/validation
```

Physical placement of a property value in a Bootstrap YAML file does not by
itself make Bootstrap the semantic owner of that property.

## Bootstrap/global ownership

Bootstrap owns runtime composition concerns such as:

```text
spring.datasource.*
spring.flyway.*
spring.security.*
server.servlet.session.*
springdoc.*
observability/runtime assembly
profile composition
```

Business migrations in Bootstrap are forbidden.

## Domain configuration

Domain-specific settings remain owned by the relevant module:

```text
sixpay.partner.*        -> Partner
sixpay.customer.*       -> Customer
sixpay.payment.*        -> Payment
sixpay.accounting.*     -> Accounting
sixpay.reporting.*      -> Reporting
sixpay.notification.*   -> Notification
sixpay.security.*       -> Security
sixpay.administration.* -> Administration
```

A module must not consume another domain's configuration namespace directly.

## Authentication

Bootstrap owns OAuth2/session runtime assembly.

Security owns the semantics/defaults/validation of:

```text
sixpay.security.authentication.*
sixpay.security.local.password.*
```

Reviewed profile semantics:

```text
local-auth
    local = enabled
    oidc  = disabled

hybrid-auth
    local = enabled
    oidc  = enabled
```

## Springdoc/OpenAPI

Bootstrap owns runtime Springdoc and `GroupedOpenApi` assembly.

Canonical runtime groups are:

```text
partner
customer
payment
administration
reporting
accounting
```

The Payment timeline endpoint is intentionally excluded from the Payment group
and exposed in the Reporting group.

Runtime Springdoc output is implementation documentation, not the contractual
source of truth.

Contractual truth remains:

```text
documentation/contracts/CONTRACT_REGISTRY.yaml
+
canonical physical contracts
```

## Angular environments

Canonical environment matrix:

```text
production  -> api  / local + OIDC
integration -> api  / local
development -> mock / standalone
netlify     -> mock / standalone
```

Production and integration must never silently fall back to mock data.

## Feature flags

`documentation/architecture/configuration/FEATURE_FLAG_REGISTRY.yaml` is the
canonical classification/ownership registry for runtime toggles.

It is not consumed by the application at runtime.

Runtime values remain in implementation configuration.

Unqualified toggle names such as:

```text
enabled
metrics-enabled
retention-enabled
```

are not canonical registry keys; full namespaces are required.

## Non-regression

Configuration changes must preserve:

```text
explicit ownership
stable profile semantics
no cross-domain configuration consumption
no API-to-mock fallback
no duplicate competing defaults
```

Permanent enforcement is implemented through the FS-2.5 architecture tests and
verification scripts.
