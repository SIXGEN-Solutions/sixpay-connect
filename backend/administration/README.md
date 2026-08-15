# Administration

The `administration` module is the ADMIN-only HTTP administration boundary for SIXPAY CONNECT. The golden structural reference remains `backend/partner`. Administration does not duplicate Security business logic or persistence.

## Ownership

```text
administration/
    ADMIN-only HTTP endpoints
    request validation
    mapping to Security administration use cases

security/
    canonical SIXPAY user
    Local credentials
    OIDC identities
    roles and permissions
    security operational audit
    user-administration application service
    security persistence
```

The central rule is: `IdP proves identity. SIXPAY owns business authorization.`

## User Administration API

Base path: `/internal/api/v1/administration/users`

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

All endpoints require the SIXPAY `ADMIN` role.

## Model

A canonical account is distinct from authentication identities. A user may have both Local and OIDC identities without duplicating the account or authorization assignments.

## Tests

```text
SecurityUserAdministrationControllerTest
SecurityUserAdministrationServiceTest
IntegrationSecurityUserSeederTest
```

Detailed evidence is maintained in `ADMINISTRATION-TEST-COVERAGE.md`.
