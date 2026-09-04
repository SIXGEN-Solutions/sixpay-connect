# FS-1.4.0 — Administration / Incidents Ownership Baseline

Status: **DECIDED**

Scope: documentation and ownership only.

Authoritative revision policy:

```text
TASK_INVOCATION_OR_EXECUTION_ENVIRONMENT
```

Golden business-module reference:

```text
backend/partner
```

## 1. Purpose

FS-1.4 closes the remaining full-stack contract gaps for:

- Operational Administration;
- Operational Incidents.

Before defining contracts or implementing endpoints, ownership must be
unambiguous.

This baseline makes no HTTP-signature, persistence-schema or permission-model
changes.

## 2. Decision

The owning backend module is:

```text
backend/administration
```

The module contains three distinct boundaries:

```text
Administration
├── Security User Administration
│   └── existing ADMIN-only HTTP boundary
│
├── Operational Administration Query
│   ├── overview
│   ├── settings
│   └── integrations
│
└── Incident Query
    ├── incidents
    └── incident detail
```

## 3. Administration owns

The `administration` module owns:

- administrative HTTP boundaries;
- operational configuration projection;
- integration-health projection;
- operational incident querying.

This ownership means Administration is responsible for exposing these
capabilities through its API/application boundary and for coordinating access to
the appropriate underlying source.

It does **not** imply that Administration becomes owner of Security data or of
all operational data in SIXPAY.

## 4. Security remains owner of

The `security` module remains the source of truth for:

- users;
- identities;
- roles;
- permissions;
- authentication.

Security also remains responsible for its existing security persistence and
authorization business rules.

Administration may expose Security User Administration endpoints, but it does
not duplicate the Security aggregate, persistence model or authorization logic.

The established rule remains:

> IdP proves identity. SIXPAY owns business authorization.

## 5. Reporting remains owner of Payment audit

The `reporting` module continues to own:

- immutable Payment audit querying;
- Payment timeline query;
- Payment audit search/detail;
- controlled audit export.

Operational Incidents are **not** moved to Reporting.

Reason:

```text
Payment audit
    = immutable evidence / audit concern

Operational incident
    = supervision / operational administration concern
```

Reusing Reporting as a generic operational module would blur the existing
bounded-context responsibility.

## 6. Operational Administration Query boundary

The capability names reserved by this baseline are:

```text
overview
settings
integrations
```

Their concrete HTTP paths and schemas are intentionally deferred to FS-1.4.1.

The following rules are already fixed:

1. Settings must project real SIXPAY configuration/runtime values.
2. Mock/demo constants are not authoritative data.
3. Integration state must come from observable runtime/integration sources.
4. The query capability is read-oriented unless a later requirement explicitly
   introduces mutation.
5. Administration must not introduce a second Security user-management model.

## 7. Incident Query boundary

The Incident Query capability owns:

```text
incident search
incident detail
```

The concrete API, filters, DTOs, persistence model and lifecycle are defined in
the following FS-1.4 steps.

This baseline does not declare mock incident fixtures as production master data.

## 8. Dependency direction

Allowed conceptual collaboration:

```text
Administration HTTP/API
        ↓
Administration application/use cases
        ↓
Administration-owned ports
        ↓
real configuration/runtime/integration sources
```

For Security user administration:

```text
Administration HTTP/API
        ↓
Security administration application capability
        ↓
Security domain/persistence
```

Forbidden direction:

```text
Administration
        ↓
duplicate Security persistence/domain
```

and:

```text
Incidents
        ↓
Reporting solely as a reuse shortcut
```

## 9. Co-deployment rule

SIXPAY currently follows the modular-monolith rule from `ENGINEERING_CONTEXT.md`:

> co-deployed internal calls remain in-process unless a deployment decision says
> otherwise.

Therefore FS-1.4 must not introduce internal HTTP calls between Administration,
Security and Reporting merely because they are separate modules.

## 10. Contract gate

No new operational Administration or Incident endpoint may be implemented until
its internal contract is published or explicitly marked `TO_DEFINE`.

Next planned steps:

```text
FS-1.4.1 — Administration Query contract
FS-1.4.2 — Incident Query contract
FS-1.4.3 — Contract registry
```

## 11. Consequences

### Positive

- ownership is explicit;
- Reporting is kept focused on Payment audit;
- Security remains the single source of truth for identity/authorization;
- Administration gains a clear operational-query responsibility;
- later Angular API wiring has a defined backend owner;
- future persistence decisions can be made without cross-domain ambiguity.

### Constraints

- Administration cannot invent configuration values to match current mocks;
- Incident persistence must be designed explicitly if Incidents become
  production-backed;
- permissions for the new operational query APIs remain to be defined by their
  contracts;
- no runtime behavior changes in FS-1.4.0.

## 12. Exit criteria

FS-1.4.0 is complete when:

- `backend/administration/README.md` reflects the three Administration
  boundaries;
- Administration ownership is explicit;
- Security ownership is explicit;
- Reporting remains explicitly limited to Payment audit/query/export;
- Incident Query ownership is assigned to Administration;
- no runtime code, API signature or database schema has been changed.

Result:

```text
OWNERSHIP NON-AMBIGUOUS
```
