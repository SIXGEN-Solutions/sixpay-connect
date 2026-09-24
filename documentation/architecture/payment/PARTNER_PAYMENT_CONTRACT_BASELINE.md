# SIXPAY CONNECT — Canonical Partner Model and Contract Baseline

## Status

Current-state architecture baseline for the Partner-agnostic external Payment boundary.

## Canonical vocabulary

- `Partner`: external organization registered in SIXPAY.
- `CanonicalPartnerIdentity`: canonical SIXPAY Partner UUID resolved by Partner/Security and consumed by Payment.
- `PaymentSource`: opaque stable source identifier; Payment does not enumerate external providers.
- `externalPaymentReference`: immutable Partner-originated reference for one logical external payment.
- `partnerExtensions`: optional bounded Partner-specific extension map governed by a registered Partner integration profile.

No external Payment capability may branch on a concrete provider name.

## D2 — Direct replacement

Generic Partner contracts replace provider-named Payment contracts directly.
Provider-named Payment contracts leave the active baseline when the registry transition is applied.
No compatibility facade named after the former provider is retained.

The exact lifecycle value used for replaced registry records must be an existing lifecycle value already supported by repository governance.

## D4 — Dynamic Partner extensions

### Value types
Allowed scalar JSON values: string, number, boolean and null.
Nested objects and arrays are forbidden.

### Structure
Keys are accepted only when allowlisted in the registered Partner integration profile.

### Cardinality
Maximum: 20 extension entries per Payment request.

### Persistence
The complete accepted extension snapshot is persisted with Payment.

### Idempotency
Only definitions marked `idempotencySignificant=true` participate in the canonical idempotency fingerprint.

### Query exposure
Only definitions marked `queryExposable=true` may be returned by Payment query/recovery surfaces.

### Callback exposure
Only definitions marked `callbackExposable=true` may be included in outbound callbacks.

### Security
Unknown keys are rejected.
Secrets, credentials, tokens and disallowed sensitive values are rejected.

## Ownership

Partner owns Partner identity and registration/profile semantics.
Security owns authentication and authorization enforcement.
Payment owns Payment request semantics and the persisted extension snapshot.
Integration remains provider-neutral and does not own Partner payloads or mappings.

## Out of scope for PA-1

- Java controller/adapter renaming.
- Security filter/guard migration.
- Callback transport implementation migration.
- Accounting external-evidence implementation migration.
- Flyway/persistence implementation for the extension snapshot.
- Frontend migration.
- Final documentation eradication.
- Permanent zero-provider naming gate.
