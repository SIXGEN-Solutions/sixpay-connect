# FS-2.5.1 — Bootstrap / Global Configuration Normalization

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.5 — Configuration consolidation`  
**Golden module:** Partner

## Purpose

FS-2.5.1 defines the canonical configuration surface owned by the Bootstrap
runtime without changing functional configuration semantics.

The guiding distinction is:

```text
Bootstrap
    = runtime configuration owner

Business module
    = business configuration semantic owner
```

## Canonical Bootstrap-owned namespaces

Bootstrap owns the runtime assembly of:

```text
server.*
spring.application.*
spring.datasource.*
spring.jpa.*
spring.flyway.*
springdoc.*
management.*
logging.*
spring.kafka.*
spring.mail.*
```

It also owns shared runtime assembly namespaces:

```text
sixpay.messaging.*
```

Authentication runtime/profile assembly remains Bootstrap-owned, while Security
policy values remain Security-owned.

## Base `application.yml`

The base configuration is the global runtime baseline.

It currently owns:

```text
server.port
spring.application.name
spring.jpa.*
spring.flyway.*
springdoc.*
management.*
sixpay.messaging.*
```

These are canonical Bootstrap/global concerns.

The following domain-owned values currently remain in the base file only as
**explicitly tracked transition debt**:

```text
sixpay.customer.verification.banking.enabled
sixpay.security.local.password.*
```

They are not reclassified as Bootstrap-owned.

They are preserved temporarily to avoid changing runtime defaults before
FS-2.5.2 establishes domain-owned configuration loading.

## Transitional-debt rule

FS-2.5.1 freezes the exact current debt.

No new `sixpay.<domain>.*` key may be added to the base `application.yml`.

Allowed transitional prefixes are only:

```text
sixpay.customer.verification.banking.*
sixpay.security.local.password.*
```

FS-2.5.2 must decide their final domain-owned loading mechanism and then remove
this temporary allow-list.

## Domain modules must not own global runtime configuration

Business modules must not introduce their own:

```text
server.*
spring.datasource.*
spring.jpa.*
spring.flyway.*
springdoc.*
management.*
```

The runtime application owns these globally.

A future extracted microservice may own those namespaces in its own deployable
runtime, but while the module is part of the SIXPAY modular monolith it must not
silently introduce a second runtime configuration root.

## Stable runtime invariants

FS-2.5.1 protects the existing effective baseline:

```text
spring.jpa.hibernate.ddl-auto = validate
spring.flyway.schemas = sixpay
spring.flyway.default-schema = sixpay
spring.flyway.locations = classpath:db/migration
spring.flyway.validate-on-migrate = true
spring.flyway.clean-disabled = true
springdoc disabled by default
management base exposure = health,info
```

Profile-specific overrides remain valid and are reviewed later in FS-2.5.3.

## Non-regression policy

FS-2.5.1 does not:

- rename property keys;
- rename environment variables;
- change defaults;
- delete profiles;
- move domain values between files;
- alter authentication mode;
- alter feature-flag behavior.

The workflow is:

```text
classify
  -> freeze global ownership
  -> prevent new debt
  -> relocate domain semantics in FS-2.5.2
  -> validate profiles in FS-2.5.3
```

## Exit criteria

FS-2.5.1 is DONE when:

- the Bootstrap global namespace whitelist is documented;
- base `application.yml` global invariants are protected;
- existing domain debt in the base file is frozen, not expanded;
- business modules cannot introduce global runtime namespaces;
- no runtime defaults are changed.
