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
production  -> api  / LDAP enabled, LOCAL + OIDC disabled by default
integration -> api  / LOCAL enabled, OIDC + LDAP disabled by default
development -> mock / standalone
netlify     -> mock / standalone
```

Production's current frontend defaults select LDAP authentication alone.
The Angular environment gate checks that at least one human authentication
capability (LOCAL, OIDC or LDAP) is enabled, rather than requiring a specific
combination. The existing runtime validator rejects production standalone
authentication and an empty set of enabled capabilities; OIDC authority,
clientId and scope remain mandatory whenever OIDC is enabled. Integration,
development and netlify retain their existing environment policies.

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

## Exceptions controlees de doublons inter-profils

Le gate `scripts/verify_spring_configuration_hygiene.py` conserve ses controles
sur les cles repetees **dans un meme fichier**, les imports `classpath` non
resolus, les fichiers runtime identiques, ainsi que les doublons inter-fichiers
non autorises.

Deux doublons historiques de valeurs identiques sont admis uniquement pour
leurs **paires exactes** de fichiers :

| Propriete | Paire autorisee | Justification |
| --- | --- | --- |
| `sixpay.customer.verification.banking.enabled` | `application-amplitude-sandbox.yml` et `application-customer-banking.yml` | Le sandbox importe la configuration Customer Banking et reitere son activation. |
| `sixpay.reporting.audit-export.retention` | `application-integration.yml` et `application-preproduction.yml` | Les deux profils conservent le meme defaut explicite de retention des exports d'audit. |

Ces exceptions ne permettent pas la meme valeur dans un troisieme fichier :
la presence d'un autre declarant est toujours signalee. Elles ne constituent
pas une approbation de profils Spring actifs simultanement et n'alterent
aucune propriete runtime, aucun contrat, ni aucune regle metier.

Le profil `production`, dont les parametres ont ete consolides dans
`application-production.yml`, releve de l'exception d'autonomie documentaire
preparee separement. Les exceptions ci-dessus restent independantes de celle-ci.
