# SIXPAY CONNECT — Module Boundaries

## Status

Canonical current-state architecture document.

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

`CustomerSubscription` is a Customer capability. There is no autonomous
`subscription` business module.

Bootstrap is the runtime assembler, not a business owner.
Integration provides only provider-neutral technical capabilities.

## Allowed cross-module surfaces

Cross-module dependencies are allowed only through reviewed public surfaces:
application ports, public application/domain contracts, shared-kernel value
objects, domain/integration events, provider-neutral integration capabilities
and reviewed Security public surfaces.

## Forbidden dependencies

A business module must not depend directly on another module's infrastructure,
JPA entity types, Spring Data repositories, persistence adapters or internal
implementation classes.

Circular business-module dependencies are forbidden.

## Bootstrap bridges

Bootstrap may implement composition adapters where two modules must be wired
without either module depending on the other's internals. Bootstrap must not
contain business logic.

## Persistence boundary

Database table ownership follows the owning business module. Cross-domain
repository access is forbidden even when modules share the same PostgreSQL
database.

## Golden reference

`backend/partner` remains the golden module for structure and implementation
conventions.

## Permanent enforcement

Module-boundary non-regression is enforced by architecture tests and repository
verification gates.
