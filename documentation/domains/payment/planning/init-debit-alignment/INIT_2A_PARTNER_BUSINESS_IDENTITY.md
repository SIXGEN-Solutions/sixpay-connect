# INIT-2A — Partner business identity

Reference revision: `3857f80711e5e7d5c36abd627d00f6aa9e7d79e1`.

## Decision

`PartnerId` remains the internal technical UUID.

`partnerIdentifier` is the mandatory, stable and unique business identity owned
by Partner. For the target Initiate Debit semantics, request `AppID` represents
this canonical Partner business identifier.

## Implementation scope

INIT-2A adds domain ownership, persistence, PostgreSQL uniqueness and a public
Partner resolution surface.

Direct access to Partner persistence from another module remains forbidden.

## Development database

Existing Partner rows cannot be assigned a synthetic business identifier.
Development databases containing pre-INIT-2A Partner rows must be recreated.

## Out of scope

Security/machine identity -> Partner binding, Payment orchestration and the
physical external Payment contract/security amendment remain outside INIT-2A.
