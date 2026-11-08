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

Bootstrap owns OAuth2/session/LDAP physical runtime assembly. Security owns the
semantics and validation of Security configuration.

Human authentication is capability-based:

```text
sixpay.security.authentication.local.enabled
sixpay.security.authentication.oidc.enabled
sixpay.security.authentication.ldap.enabled
```

All non-empty provider combinations are supported. A secured runtime with all three providers disabled is invalid. Technical runtimes such as `standalone` may intentionally run without a human authentication provider.

Existing runtime profiles are preserved; provider combinations must not be
materialized as a separate YAML profile matrix. Reusable runtime fragments under
`config/security/` carry provider-specific physical configuration.

LDAP secrets are runtime-injected and have no repository default.

Initial privileged provisioning follows the same ownership boundary: Security
owns the one-shot LDAP administrator bootstrap semantics, transaction and
persistence ports; Bootstrap may only trigger that Security application port
from runtime configuration. Bootstrap must never access Security repositories,
JPA entities or infrastructure adapters directly. The bootstrap is disabled by
default, requires an empty canonical user store, provisions no LOCAL credential,
and must fail closed if left enabled after initial provisioning.

## Springdoc/OpenAPI

Bootstrap owns runtime Springdoc and `GroupedOpenApi` assembly.

Runtime groups must reflect the actual inbound SIXPAY HTTP surfaces. The
Payment timeline belongs to Reporting rather than Payment.

Runtime Springdoc output is implementation documentation, not contractual source
of truth. Contractual truth remains the contract registry plus canonical
physical contracts.

## Angular environments

The Angular authentication model supports the same three human capabilities as
the backend:

```text
LOCAL
OIDC
LDAP
```

The committed environment files contain deployment defaults, not the complete
capability matrix. At the current baseline:

```text
production  -> api  / LOCAL + OIDC enabled, LDAP disabled by default
integration -> api  / LOCAL enabled, OIDC + LDAP disabled by default
development -> mock / standalone
netlify     -> mock / standalone
```

LDAP may be enabled for an applicable API-backed deployment only when its
backend runtime/trust configuration is available. Backend session capabilities
become authoritative after session establishment.

Production and integration must never silently fall back to mock data.

## Feature flags

`documentation/architecture/configuration/FEATURE_FLAG_REGISTRY.yaml` is the
canonical classification/ownership registry for runtime toggles. Runtime values
remain in implementation configuration.

## Production runtime configuration consolidation

`backend/bootstrap/src/main/resources/application-production.yml` is the
self-contained runtime configuration for the `production` Spring profile. It
intentionally repeats values also declared in the global `application.yml` and
in other, separately selected profiles or reusable configuration fragments.
This duplication is the consequence of the approved production deployment
composition; it is not by itself a competing runtime default. The production
profile does not rely on `spring.config.import` for those fragments.

This exception concerns **only** identical `(property key, value)` declarations
where one file is `application-production.yml`. It does not permit duplicate
keys inside a YAML/properties file or identical runtime configuration files.
It also does not permit duplicate values shared by two or more *other* runtime
files. Explicit classpath imports must still resolve. All module ownership,
security, feature-flag and profile-safety requirements remain unchanged.

Spring Boot can still load `application.yml` together with a selected profile.
The production consolidation does not imply that global configuration loading
is disabled; deployed values and overrides remain subject to Spring Boot
precedence and must be reviewed when the production profile is configured.

## Non-regression

Configuration changes must preserve explicit ownership, stable profile
semantics, no cross-domain configuration consumption, no API-to-mock fallback
and no duplicate competing defaults outside the narrowly documented production
consolidation exception.

```bash
python scripts/verify_spring_configuration_hygiene.py
```
