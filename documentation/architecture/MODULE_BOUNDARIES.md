# SIXPAY CONNECT — Module Boundaries

## Status

Canonical current-state architecture document.

This document contains the stable conclusions previously established during
the FS-2.4 module-boundary audit. The phase-specific audit documents were
removed after their durable rules were absorbed here and into permanent gates.

## Architectural rule

SIXPAY CONNECT is a modular monolith whose business modules must remain
extractable without rewriting their domain logic.

Business modules:

```text
Partner
Customer
Payment
Accounting
Reporting
Notification
Security
Administration
```

Bootstrap is the runtime assembler, not a business owner.

Integration provides only provider-neutral technical capabilities.

## Allowed cross-module surfaces

Cross-module dependencies are allowed only through reviewed public surfaces:

```text
application port
public application model/command
public domain contract
shared-kernel value object
domain/integration event
provider-neutral integration capability
reviewed Security public surface
```

The repository currently accepts both package conventions:

```text
application.port.in
application.port.input
```

as equivalent public input-port naming until repository-wide naming is
consolidated separately.

## Forbidden dependencies

A business module must not depend directly on another module's:

```text
infrastructure package
JPA entity
Spring Data repository
persistence adapter
internal implementation class
```

Circular business-module dependencies are forbidden.

## Security surfaces

Reviewed Security public surfaces such as authentication context, roles,
permissions and user-administration application contracts are legitimate
cross-module dependencies when required by the owning use case.

Importing a public Security Java type is **not** equivalent to consuming
` sixpay.security.* ` runtime configuration.

## Bootstrap bridges

Bootstrap may implement cross-module composition adapters where two modules
must be wired without either module depending on the other's internals.

Bootstrap must not become a location for business logic.

## Persistence boundary

Database table ownership follows the owning business module.

Cross-domain repository access is forbidden even when modules share the same
physical PostgreSQL database.

## Golden reference

`backend/partner` remains the golden module for structure and implementation
conventions.

## Permanent enforcement

Module-boundary non-regression is enforced by architecture tests/gates.
Detailed gate implementation remains in the repository tests and verification
scripts rather than being duplicated in this document.
