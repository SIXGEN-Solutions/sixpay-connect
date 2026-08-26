# Common Module

## Purpose

Common contains small framework-independent technical contracts shared across
SIXPAY CONNECT modules.

Examples include correlation identifiers, time and identifier providers,
Outbox source contracts and other primitives that do not belong to a single
business domain.

Common must remain domain-neutral and must not contain business rules,
provider mappings or business persistence.
