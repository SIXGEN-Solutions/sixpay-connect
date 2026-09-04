# SIXPAY CONNECT — DA-0 Dual Authentication Baseline & Architecture Conformance

## 1. Purpose

This document formalizes the existing SIXPAY CONNECT authentication baseline and the target architecture for the dedicated **Dual Authentication — Local + OIDC** phase.

DA-0 is intentionally documentation-only. It defines architecture, ownership, invariants, conformance rules and no-change boundaries before any implementation change.

The authoritative revision policy for this capability is:

```text
TASK_INVOCATION_OR_EXECUTION_ENVIRONMENT
```

The repository entry point remains:

```text
ENGINEERING_CONTEXT.md
```

The `partner` module remains the golden business-module reference for implementation discipline and folder-structure conventions. Security assets remain owned by the `security` module and MUST NOT be moved under `partner`.

---

## 2. Repository constraints

The following repository rules apply to this phase:

1. Preserve module boundaries.
2. Reuse existing implementation patterns before introducing new abstractions.
3. Keep authentication and authorization concerns inside the security boundary.
4. Keep provider-specific concerns outside business modules.
5. Business modules MUST NOT depend on the selected authentication mechanism.
6. Documentation, implementation, tests and validation MUST evolve together.
7. Consistency takes precedence over creativity.

---

## 3. Current authentication baseline

### 3.1 Current frontend model

The frontend currently defines three mutually exclusive modes:

```text
Authentication
├── standalone
├── local
└── oidc
```

The current configuration model is conceptually:

```text
mode = standalone
   OR local
   OR oidc
```

This means Local and OIDC cannot currently be enabled simultaneously.

### 3.2 Current frontend responsibilities

The current frontend already contains:

```text
frontend/src/app/core/auth/
├── authentication.service.ts
├── authentication.guard.ts
├── authentication.model.ts
├── local-authentication.client.ts
└── login.component.ts
```

Existing behavior:

- `standalone` initializes a simulated/demo identity.
- `local` restores a SIXPAY local session through `/api/v1/auth/me`.
- `oidc` initializes authentication through the configured OIDC client.
- the login page displays either Local authentication or OIDC authentication;
- the route guard depends on `isAuthenticated()` rather than the authentication mechanism;
- return URL validation is already centralized in the authentication service.

### 3.3 Current backend baseline

The canonical security module is:

```text
backend/security/
```

Its responsibility is authentication, authorization and shared Spring Security infrastructure.

The current module already contains Spring Security and OAuth2 Resource Server support. Existing security coverage already treats the following concerns as security-owned:

```text
Core model / policies
Authentication boundary
JWT conversion
HTTP security infrastructure
```

The existing Local frontend integration already expects:

```text
POST /api/v1/auth/login
GET  /api/v1/auth/me
POST /api/v1/auth/logout
```

DA-0 preserves these existing boundaries unless a later implementation lot demonstrates a concrete need to evolve them.

---

## 4. Target authentication capability model

`standalone` remains a development/demo capability only.

Production authentication becomes capability-based instead of mode-based:

```text
production:
  local.enabled = true|false
  oidc.enabled  = true|false
```

The valid production combinations are:

| Local | OIDC | Result |
|---|---|---|
| false | false | INVALID |
| true | false | Local only |
| false | true | OIDC / SSO only |
| true | true | Hybrid Local + OIDC |

### 4.1 Target conceptual configuration

Backend:

```yaml
sixpay:
  security:
    authentication:
      local:
        enabled: true
      oidc:
        enabled: true
```

Frontend target capability model:

```text
standalone = dev/demo only

local.enabled
oidc.enabled
```

The frontend MUST no longer infer that Local and OIDC are mutually exclusive.

### 4.2 Production invariant

The following state is invalid in production:

```text
local.enabled = false
oidc.enabled  = false
```

A later implementation lot MUST introduce fail-fast validation for this invalid production configuration.

---

## 5. Separation of responsibilities

The target architecture is:

```text
LOCAL authentication ──┐
                       │
                       ├──> Canonical SIXPAY Identity
                       │           │
OIDC authentication ───┘           ▼
                            Roles / Permissions
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
                 partner         payment         customer
                                    │
                           accounting / reporting
```

### 5.1 Authentication responsibility

Authentication answers:

```text
Who is the caller?
```

Owned by:

```text
backend/security
frontend/core/auth
bootstrap/deployment configuration
```

### 5.2 Authorization responsibility

Authorization answers:

```text
What is this authenticated SIXPAY user allowed to do?
```

Business roles and permissions remain SIXPAY-owned.

OIDC providers may provide identity attributes and optionally external groups, but provider claims MUST NOT create direct dependencies from business modules to the external Identity Provider.

### 5.3 Business module responsibility

Business modules consume:

```text
authenticated identity
roles
permissions
```

Business modules MUST NOT consume:

```text
authentication mode
OIDC provider name
OIDC issuer
OIDC client
password credentials
local authentication state
provider-specific claims
```

---

## 6. Canonical identity direction

DA-0 establishes the direction for DA-1:

```text
Local authentication ──┐
                       ├──> Unified SIXPAY Principal
OIDC authentication ───┘
```

The target MUST NOT propagate separate authentication-specific principals such as:

```text
LocalPrincipal
OidcPrincipal
```

into business modules.

The canonical SIXPAY identity will be defined in DA-1.

---

## 7. Ownership matrix

| Concern | Current implementation | Target ownership | Required change | No-change boundary |
|---|---|---|---|---|
| Standalone authentication | Frontend auth mode | Frontend dev/demo only | Keep outside production auth model | Business modules |
| Local login | Frontend + `/api/v1/auth/*` | `frontend/core/auth` + `backend/security` | Integrate as an independently enabled capability | Business modules |
| OIDC login | Frontend OIDC client | `frontend/core/auth` + `backend/security` | Make independently enableable and coexist with Local | Business modules |
| Authentication configuration | Exclusive frontend `mode` | Security/bootstrap + frontend capability model | Replace production exclusivity with Local/OIDC flags | Business modules |
| Route authentication guard | Frontend core auth | Frontend core auth | Preserve mechanism independence | All business routes |
| HTTP security | `backend/security` | `backend/security` | Extend only where hybrid support requires it | Business modules |
| JWT validation/conversion | `backend/security` | `backend/security` | Reuse and normalize into canonical identity | Business modules |
| Local session | Local auth flow | `backend/security` | Preserve and normalize | Business modules |
| OIDC session | OIDC frontend flow | Security boundary | Normalize with Local identity/session semantics | Business modules |
| Canonical principal | Partially implicit | `backend/security` | Define in DA-1 | All business modules consume only normalized identity |
| Identity linking | Not formally defined | `backend/security` | Define in later lot | Business modules |
| Roles/permissions | SIXPAY | SIXPAY security/authorization boundary | Preserve provider neutrality | Business modules |
| Provider configuration | Environment/deployment | Bootstrap/deployment/secrets | Externalize provider-specific settings | Business modules |
| Authentication audit | Security concern | `backend/security` / audit infrastructure | Add in later lot | Business logic |
| User auth administration | Administration/security boundary | Administration + security ports | Add later without provider coupling | Business domains |

---

## 8. Explicit no-change modules

The following modules MUST NOT become dependent on Local/OIDC selection:

```text
partner
payment
customer
accounting
reporting
incident
```

The same rule applies to any future business module.

Forbidden examples:

```java
if (authenticationType == LOCAL) {
    // business behavior
}

if (authenticationType == OIDC) {
    // business behavior
}
```

Forbidden provider coupling:

```java
if (issuer.contains("microsoft")) {
    // business authorization
}
```

Allowed pattern:

```text
authenticated SIXPAY identity
+ SIXPAY roles/permissions
→ business authorization
```

Existing method-security rules such as `@PreAuthorize` SHOULD remain independent of how the principal authenticated.

---

## 9. Configuration invariants

### 9.1 Development/demo

`standalone` is allowed only for controlled development/demo use.

It MUST NOT be considered a production authentication mechanism.

### 9.2 Production

At least one real authentication capability MUST be enabled.

```text
local || oidc == true
```

### 9.3 Hybrid mode

When both are enabled:

```text
local.enabled = true
oidc.enabled  = true
```

the login experience MUST expose both options while converging to one canonical SIXPAY authenticated identity.

### 9.4 Provider neutrality

The architecture MUST support OIDC as a protocol capability.

Core security contracts MUST NOT be named after a specific provider such as:

```text
EntraAuthenticationService
KeycloakPrincipal
OktaUser
```

Provider-specific adapters/configuration MAY exist at the infrastructure edge when required.

---

## 10. Security invariants

1. Passwords MUST never leave the Local authentication boundary except as transient login input.
2. Passwords MUST never be stored in clear text.
3. OIDC access tokens, refresh tokens, authorization codes and client secrets MUST never be logged.
4. Authentication method MUST NOT alter business authorization semantics.
5. Disabled SIXPAY users MUST remain disabled regardless of authentication method.
6. An externally authenticated identity MUST resolve to an authorized SIXPAY identity before business access is granted.
7. OIDC provider claims MUST NOT directly bypass SIXPAY role/permission governance.
8. Local and OIDC authentication MUST converge to the same authorization model.
9. Business APIs MUST remain protected independently of the login mechanism.
10. Security errors MUST not reveal whether a user, external identity or password record exists beyond the agreed contract.

---

## 11. Conformance findings

### CONFORMANT — canonical security module

Security is already isolated under:

```text
backend/security/
```

This is retained.

### CONFORMANT — business dependency direction

Business modules consume the shared security boundary instead of implementing authentication themselves.

This is retained.

### CONFORMANT — authentication guard abstraction

The frontend authentication guard evaluates authenticated state rather than checking Local or OIDC directly.

This is retained.

### GAP — exclusive frontend authentication mode

Current:

```text
standalone | local | oidc
```

Target:

```text
standalone = dev/demo

production capabilities:
local.enabled
oidc.enabled
```

Planned resolution: DA-2.

### GAP — login screen exclusivity

Current UI exposes Local OR OIDC.

Target UI exposes every enabled authentication capability.

Planned resolution: DA-7.

### GAP — session bootstrap tied to exclusive mode

Current session initialization chooses one branch according to a single authentication mode.

Target session initialization must support hybrid capability discovery/normalization.

Planned resolution: DA-8.

### GAP — authentication-specific logout/token handling

Current logout/token behavior is selected according to the exclusive mode.

Target behavior must be based on the active authenticated session/mechanism without leaking that concern to business code.

Planned resolution: DA-8.

### GAP — canonical SIXPAY principal not formally defined

A common business-facing principal must be explicitly defined.

Planned resolution: DA-1.

### GAP — production capability validation not formalized

The invalid state:

```text
local.enabled = false
oidc.enabled = false
```

must fail fast in production.

Planned resolution: DA-2.

### GAP — external identity linking not formalized

The relationship between an external OIDC identity and a SIXPAY user must be explicitly modelled.

Planned resolution: later Identity Linking lot.

---

## 12. No implementation changes in DA-0

DA-0 MUST NOT:

- modify Java production code;
- modify Angular production code;
- change database schema;
- change authentication contracts;
- change business authorization behavior;
- introduce provider-specific classes;
- alter business modules.

DA-0 is complete when the architecture baseline and conformance rules are accepted.

---

## 13. DA-0 exit criteria

DA-0 is considered complete when all of the following are true:

- [x] Existing authentication modes are documented.
- [x] Current exclusivity is documented.
- [x] Target Local/OIDC capability model is documented.
- [x] All four production combinations are classified.
- [x] Invalid production configuration is identified.
- [x] Authentication ownership is explicit.
- [x] Authorization ownership is explicit.
- [x] Provider-neutrality rule is explicit.
- [x] Business no-change boundary is explicit.
- [x] Canonical identity convergence is established as the DA-1 direction.
- [x] Known architecture gaps are mapped to later implementation lots.
- [x] No business change is required for DA-0.
- [x] No production code modification is required for DA-0.

---

## 14. Architecture decision

**Decision:** SIXPAY CONNECT will evolve from an exclusive authentication-mode model to a capability-based authentication model.

Production MAY enable Local authentication, OIDC authentication, or both.

Both authentication mechanisms MUST converge to one canonical SIXPAY identity and one SIXPAY-owned authorization model.

Business modules MUST remain independent of authentication mechanism and Identity Provider.

`standalone` remains development/demo-only.

---

## 15. Next implementation lot

The next lot is:

```text
DA-1 — Unified SIXPAY Principal
```

Its responsibility is to define the canonical authenticated identity contract consumed by authorization and business boundaries without exposing Local/OIDC implementation details.
