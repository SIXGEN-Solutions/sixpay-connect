# FS-2.5.8 — Configuration Non-Regression Gate

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.5 — Configuration consolidation`  
**Golden module:** Partner

## Purpose

FS-2.5.8 does not redefine the detailed configuration rules already established
in FS-2.5.1 through FS-2.5.7.

It provides one canonical orchestration gate:

```text
scripts/verify_configuration_consolidation.py
```

The gate executes the existing backend, frontend and feature-flag controls as
one configuration baseline.

## Covered policies

### Bootstrap/global configuration

Protects:

- base runtime invariants;
- Bootstrap global namespace ownership;
- absence of new domain configuration debt in the base application file.

### Domain configuration ownership

Protects:

- `sixpay.<domain>.*` semantic ownership;
- absence of direct cross-domain configuration consumption.

### Runtime profiles

Protects:

- canonical Flyway locations;
- absence of historical migration paths;
- local/hybrid authentication profile semantics;
- absence of destructive Hibernate schema profiles.

### Security/authentication

Protects:

- Security-owned property binding/defaults;
- Bootstrap-owned OAuth2/session runtime assembly;
- no direct foreign consumption of `sixpay.security.*` configuration.

### OpenAPI/Springdoc

Protects:

- Bootstrap-owned `GroupedOpenApi` assembly;
- canonical OpenAPI groups;
- Payment timeline ownership by Reporting;
- Springdoc disabled by default and enabled only in reviewed profiles.

### Angular environments

Protects:

- production/integration API-only policy;
- development/netlify explicit mock policy;
- authentication environment matrix;
- Angular CLI file-replacement mappings;
- absence of API-to-mock fallback.

### Feature-flag registry

Protects:

- explicit ownership;
- namespace/owner consistency;
- no `REVIEW_REQUIRED` owners;
- no unqualified parser artifacts;
- reviewed Angular/Security/runtime flags.

## Gate composition

The canonical gate executes:

```text
Backend architecture tests
    BootstrapGlobalConfigurationArchitectureTest
    DomainConfigurationOwnershipArchitectureTest
    RuntimeProfileConfigurationArchitectureTest
    SecurityAuthenticationConfigurationArchitectureTest
    OpenApiSpringdocConfigurationArchitectureTest

Frontend
    verify:angular-environments
    verify:runtime-datasource-policy

Repository
    verify_feature_flag_registry.py
```

## Important design rule

This gate orchestrates existing rules. It does not duplicate them.

```text
detailed rule
    = owning architecture test / verifier

FS-2.5.8
    = orchestration + completeness check
```

This prevents two independent implementations of the same configuration rule.

## Non-regression policy

A gate failure means:

```text
detect
  -> inspect
  -> prove regression
  -> minimal correction
  -> rerun gate
```

It does not authorize automatic changes to functional code, property defaults,
environment variables, profiles or authentication behavior.

## Exit criteria

FS-2.5.8 is complete when:

- all FS-2.5 architecture artifacts exist;
- the consolidated gate executes successfully;
- backend configuration architecture tests pass;
- frontend environment/runtime datasource gates pass;
- feature-flag registry validation passes.
