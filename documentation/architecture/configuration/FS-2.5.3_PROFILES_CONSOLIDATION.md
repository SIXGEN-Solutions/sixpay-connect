# FS-2.5.3 — Runtime Profiles Consolidation

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.5 — Configuration consolidation`  
**Golden module:** Partner

## Purpose

FS-2.5.3 normalizes the role of Bootstrap `application-*.yml` files without
renaming existing profiles or changing functional defaults unnecessarily.

A Spring profile is a runtime composition, not a business capability owner.

## Canonical profile categories

- `BASE_RUNTIME`: `application.yml`
- `ENVIRONMENT_RUNTIME`: executable environment profiles such as standalone/integration
- `AUTHENTICATION_COMPOSITION`: local-auth / hybrid-auth
- `TRANSPORT_COMPOSITION`: Kafka/runtime transport composition
- `SANDBOX_COMPOSITION`: provider/capability sandbox fixtures
- `CAPABILITY_COMPOSITION`: capability-specific composition profiles

## Critical Flyway normalization

`classpath:db/security/migration` is obsolete after FS-2.3.

All canonical migrations now come from:

```text
classpath:db/migration
```

Therefore `application-integration.yml` must no longer include the old Security
migration path.

## Profile safety rules

- No profile may restore `db/security/migration`.
- No profile may enable `baseline-on-migrate: true`.
- No profile may reference historical `V2026...` migrations.
- No profile may introduce destructive Hibernate `create` or `create-drop`.
- Existing standalone `ddl-auto=update` developer default is preserved for now.
- `local-auth` keeps local enabled / OIDC disabled.
- `hybrid-auth` keeps local enabled / OIDC enabled.
- Domain values in profiles remain semantically domain-owned per FS-2.5.2.

## Non-regression policy

FS-2.5.3 does not rename or delete profiles merely for cleanliness.
The only immediate cleanup is removal of the proven obsolete Flyway location.


## Current profile inventory

### AUTHENTICATION_COMPOSITION

- `application-hybrid-auth.yml`
- `application-local-auth.yml`

### BASE_RUNTIME

- `application.yml`

### CAPABILITY_COMPOSITION

- `application-accounting-api.yml`
- `application-accounting.yml`
- `application-customer-banking.yml`
- `application-customer-projection-outbox.yml`
- `application-notification-operational.yml`
- `application-oidc.yml`
- `application-payment-banking-compensation.yml`
- `application-payment-banking-posting.yml`
- `application-payment-banking-reservation.yml`
- `application-payment-banking-status.yml`
- `application-secured.yml`
- `application-tresorpay.yml`

### ENVIRONMENT_RUNTIME

- `application-integration.yml`
- `application-standalone.yml`

### SANDBOX_COMPOSITION

- `application-accounting-api-sandbox.yml`
- `application-amplitude-payment-sandbox.yml`
- `application-amplitude-sandbox.yml`

### TRANSPORT_COMPOSITION

- `application-integration-kafka.yml`
- `application-kafka.yml`

