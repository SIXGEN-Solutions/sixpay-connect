# FS-2.5.7 — Feature-Flag Registry

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.5 — Configuration consolidation`

## Canonical rule

`FEATURE_FLAG_REGISTRY.yaml` is the canonical classification and ownership registry for runtime toggles.

It is not consumed by the application at runtime.

Runtime values remain in implementation configuration.

## Governance

A new feature flag must identify an owner, category, runtime source, profiles and change policy before it is accepted.

Cross-domain direct configuration consumption is forbidden.

The registry does not create defaults and does not override YAML or `@ConfigurationProperties`.

## Inventory

Registered flags/toggles: **55**

### By owner

- `ACCOUNTING`: **1**
- `BOOTSTRAP_RUNTIME`: **4**
- `CUSTOMER`: **4**
- `FRONTEND_AUTH_RUNTIME`: **3**
- `FRONTEND_RUNTIME`: **1**
- `NOTIFICATION`: **8**
- `PAYMENT`: **18**
- `REVIEW_REQUIRED`: **6**
- `SECURITY`: **10**

### By category

- `AUDIT`: **1**
- `AUTHENTICATION_OR_SECURITY`: **13**
- `DOMAIN_OR_RUNTIME_FEATURE`: **28**
- `FRONTEND_DATASOURCE`: **1**
- `INTEGRATION_CALLBACK`: **3**
- `OPERATIONS`: **4**
- `RESILIENCE`: **3**
- `TRANSPORT`: **2**

## Unresolved ownership

No unresolved feature-flag ownership.

The former unqualified parser artifacts `enabled`, `metrics-enabled` and `retention-enabled` were removed. Their qualified runtime properties remain the canonical registry entries.

## Non-regression policy

FS-2.5.7 changes no runtime value, default, environment variable, profile or functional branch.
