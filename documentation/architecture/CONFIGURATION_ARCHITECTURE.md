# SIXPAY CONNECT — Configuration Architecture

## Status

Canonical current-state configuration architecture.

This document contains the stable configuration baseline established during
FS-2.5. Phase-specific consolidation documents were removed after their
durable rules were absorbed here and into permanent verification gates.

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

## Physical runtime configuration layout

`backend/bootstrap` is the sole physical owner of runtime
`application*.properties`, `application*.yaml` and `application*.yml` files.
This prevents classpath-order-dependent configuration when the modular
monolith assembles its business-module JARs.

The canonical layout is:

```text
backend/bootstrap/src/main/resources/
├── application.yml                 # global runtime baseline
├── application-<profile>.yml       # profile activation/composition
└── config/
    ├── payment/                    # reusable Payment runtime fragments
    └── security/                   # reusable Security runtime fragments
```

Business modules own their `@ConfigurationProperties` classes, validation and
semantic defaults. They do not package runtime `application*` files under
`src/main/resources`.

Module-local `src/test/resources/application-test.yml` files are retained as
test fixtures. They configure isolated test application contexts and are not
runtime competitors.

Shared values used by more than one profile must be declared once under
`bootstrap/config/` and imported explicitly. A repeated property is permitted
only when its value intentionally changes profile semantics, for example Local
versus OIDC activation.

Current reusable composition fragments are:

| Fragment | Purpose | Consumers |
|---|---|---|
| `config/payment/tresorpay-common.yml` | Shared TRESOR PAY protocol/security settings | `standalone`, `tresorpay` |
| `config/security/local-auth-common.yml` | Local-authentication limits and HTTP session assembly | `local-auth`, `hybrid-auth` |
| `config/security/oidc-common.yml` | Resource-server JWT and OIDC registration assembly | `hybrid-auth`, `secured`; `oidc` aliases `secured` |

`application-payment-banking.yml` owns the common OAuth2 client and SSL bundle
required by the Amplitude Payment sandbox profile. Capability-specific
reservation, posting, compensation and status properties remain in their
dedicated files.

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

Static Spring configuration hygiene can be checked without Maven:

```bash
python scripts/verify_spring_configuration_hygiene.py
```
