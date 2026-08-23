# Administration

The `administration` module is the HTTP administration and operational-query
boundary for SIXPAY CONNECT.

The golden structural reference remains `backend/partner`.

Administration does not duplicate Security business logic, identity ownership,
authorization ownership, or Security persistence.

## Ownership

```text
administration/
    Security User Administration HTTP boundary
        ADMIN-only endpoints
        request validation
        mapping to Security administration use cases

    Operational Administration Query
        overview projection
        settings projection
        integration-health projection

    Incident Query
        operational incident search
        operational incident detail
```

```text
security/
    canonical SIXPAY user
    Local credentials
    OIDC identities
    roles and permissions
    authentication
    security operational audit
    user-administration application service
    security persistence
```

The central security rule remains:

> IdP proves identity. SIXPAY owns business authorization.

## Explicit module responsibility

The `administration` module owns:

- administrative HTTP boundaries;
- operational configuration projection;
- integration-health projection;
- operational incident querying.

The `security` module remains the owner of:

- users;
- identities;
- roles;
- permissions;
- authentication.

Administration may invoke Security application capabilities through established
ports/use cases, but it must not replicate Security domain rules or persist
Security-owned aggregates.

## Security User Administration API

Base path:

```text
/internal/api/v1/administration/users
```

Current endpoints:

```text
POST   /users
GET    /users
GET    /users/{userId}
PUT    /users/{userId}
POST   /users/{userId}/enable
POST   /users/{userId}/disable
DELETE /users/{userId}
PUT    /users/{userId}/authentication-methods/local
POST   /users/{userId}/local-password-reset
POST   /users/{userId}/identities/oidc
DELETE /users/{userId}/identities/{identityId}
```

These existing endpoints require the SIXPAY `ADMIN` role.

## Operational Administration Query

FS-1.4 reserves the following Administration-owned read capabilities:

```text
overview
settings
integrations
```

The concrete HTTP contracts are not defined by this baseline step.
They are introduced in FS-1.4.1.

Operational configuration projection must expose only values backed by real
SIXPAY configuration/runtime sources. Mock/demo constants are not system of
record data.

Integration-health projection must expose only health or state that SIXPAY can
actually observe from its runtime/integration boundaries.

## Incident Query

Operational incident querying belongs to `administration`, not `reporting`.

The capability owns:

```text
incident search
incident detail
incident operational timeline/projection where defined by contract
```

The concrete HTTP contract and persistence model are introduced after this
ownership baseline.

## Reporting boundary

The `reporting` module remains responsible for immutable Payment audit querying
and controlled audit export.

Operational incidents are deliberately not moved to Reporting because incident
supervision is an operational administration concern rather than Payment audit
ownership.

## Boundary rules

1. `administration` owns the HTTP/operational-query boundary.
2. `security` owns canonical users, identities, roles, permissions and
   authentication.
3. `reporting` owns immutable Payment audit query/export capabilities.
4. Administration must not duplicate Security persistence or authorization
   business rules.
5. Incident querying must not depend on Reporting merely to reuse audit
   infrastructure.
6. Cross-module collaboration must use existing application boundaries/ports.
7. Co-deployed internal interactions remain in-process unless a deployment
   decision explicitly requires otherwise.
8. New HTTP signatures require a published internal contract before
   implementation.

## Model

A canonical Security account remains distinct from authentication identities.
A user may have Local and OIDC identities without duplicating the account or
authorization assignments.

Operational Administration projections and Operational Incidents are separate
concerns from the Security user aggregate.

## Tests

Existing Security User Administration coverage:

```text
SecurityUserAdministrationControllerTest
SecurityUserAdministrationServiceTest
IntegrationSecurityUserSeederTest
```

Detailed existing evidence is maintained in:

```text
ADMINISTRATION-TEST-COVERAGE.md
```

FS-1.4 will add focused contract, application, API and persistence coverage for
the operational Administration and Incident query capabilities once their
contracts are defined.
