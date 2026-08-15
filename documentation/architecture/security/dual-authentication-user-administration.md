# Dual Authentication — User Administration

## Scope

This document records the implemented administration model for SIXPAY CONNECT Local + OIDC authentication.

## Responsibility split

```text
Browser / Administration UI
        |
        v
Administration API
        |
        v
SecurityUserAdministrationUseCase
        |
        +-------------------------------+
        |                               |
        v                               v
Canonical SIXPAY account          Authentication identities
roles / permissions              Local / OIDC
        |                               |
        +---------------+---------------+
                        |
                        v
                Unified SIXPAY principal
```

Administration owns the HTTP boundary. Security owns canonical identity, authentication identities, authorization, credentials, persistence and audit. Business modules must not depend on Local or OIDC.

## Lifecycle

Supported canonical user operations are create, list/read, update profile, update roles/permissions, enable, disable and delete. Creation may optionally provision Local authentication. OIDC linking remains a separate explicit action.

## Authentication methods

Local supports enable/disable and password reset. OIDC supports explicit link/unlink. No free auto-provisioning by email is introduced. Local and OIDC may resolve to the same canonical SIXPAY user.

## Authorization

Authentication mechanisms do not own business roles. Provider groups may be mapped by controlled policy, but provider claims are not the ultimate business-authorization authority.

## Administration security and audit

All user-administration endpoints require `ROLE_ADMIN`. The authenticated administrator is resolved through the unified CurrentUserProvider/SixpayPrincipal path and propagated as audit actor.

Relevant events include USER_CREATED, USER_UPDATED, USER_ENABLED, USER_DISABLED, USER_DELETED, PASSWORD_RESET, IDENTITY_LINKED, IDENTITY_UNLINKED, AUTH_METHOD_ENABLED and AUTH_METHOD_DISABLED. Audit must never contain passwords, password hashes, access/refresh tokens, authorization codes or client secrets.

## Integration seed

The integration profile may create ADMIN, MANAGER, AUDITOR and PARTNER users. The seed is integration-only, idempotent, uses the normal Security administration use case and environment-overridable development passwords. It is not a production provisioning path.

## Frontend

The Administration UI exposes list, create, profile/authorization edit, enable/disable, delete, Local enable/disable, Local password reset, OIDC link/unlink and recent security events. It does not branch authorization rules on the active authentication mechanism.

## Validation evidence

```text
SecurityUserAdministrationServiceTest
SecurityUserAdministrationControllerTest
IntegrationSecurityUserSeederTest
SecurityUserAdministrationService.spec.ts
BackendGoldenCoverageGateTest
```
