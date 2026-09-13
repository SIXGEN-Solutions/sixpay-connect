# SIXPAY CONNECT — Configuration Architecture

## Status

Canonical current-state configuration architecture.

## Ownership model

Bootstrap owns runtime/global composition. Business modules own domain-specific
configuration semantics, defaults and validation.

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

## Runtime configuration layout

`backend/bootstrap` is the sole physical owner of runtime `application*`
properties/YAML files. Business modules own their `@ConfigurationProperties`
classes, validation and semantic defaults.

Module-local test configuration remains test-only and must not compete with
runtime configuration.

## Authentication

Bootstrap owns OAuth2/session runtime assembly. Security owns the semantics and
validation of Security configuration.

## Springdoc/OpenAPI

Bootstrap owns runtime Springdoc and `GroupedOpenApi` assembly.

Runtime groups must reflect the actual inbound SIXPAY HTTP surfaces. The
Payment timeline belongs to Reporting rather than Payment.

Runtime Springdoc output is implementation documentation, not contractual source
of truth. Contractual truth remains the contract registry plus canonical
physical contracts.

## Angular environments

```text
production  -> api  / local + OIDC
integration -> api  / local
development -> mock / standalone
netlify     -> mock / standalone
```

Production and integration must never silently fall back to mock data.

## Feature flags

`documentation/architecture/configuration/FEATURE_FLAG_REGISTRY.yaml` is the
canonical classification/ownership registry for runtime toggles. Runtime values
remain in implementation configuration.

## Non-regression

Configuration changes must preserve explicit ownership, stable profile
semantics, no cross-domain configuration consumption, no API-to-mock fallback
and no duplicate competing defaults.

```bash
python scripts/verify_spring_configuration_hygiene.py
```
