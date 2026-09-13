# SIXPAY CONNECT — LOT 5.8 Administration / Identity / Incidents — Test Coverage

## Status

```text
ADMINISTRATION = COVERED
```

## Scope

LOT 5.8 consolidates three existing operational capabilities:

- Administration operational queries;
- Security-owned user and identity administration exposed through the Administration HTTP boundary;
- read-only operational Incident queries.

Dynamic-settings mutation is explicitly outside this closure and remains a
separate decision workstream.

## Ownership invariants

```text
backend/administration
    administrative HTTP boundary
    operational Administration queries
    operational Incident query/persistence

backend/security
    canonical SIXPAY users
    authentication identities
    Local credentials
    SIXPAY authorization
    security administration use cases
    security audit
```

Administration does not take ownership of canonical users, identities, roles,
permissions or authentication.

## Implemented security model

Administration operational queries:

```text
ROLE_ADMIN
```

Security User Administration:

```text
ROLE_ADMIN
```

Operational Incident queries:

```text
ROLE_ADMIN
ROLE_MANAGER
ROLE_AUDITOR
```

No new permission or security rule was introduced by LOT 5.8 closure.

## Coverage evidence

Backend coverage includes:

- Administration query authentication and ADMIN-only authorization;
- mandatory `X-Correlation-ID` handling and response echo;
- Security User Administration authentication, ADMIN-only authorization,
  validation, actor propagation and administrative commands;
- Incident authentication and role authorization;
- Incident pagination validation;
- Incident filtering and PostgreSQL persistence;
- Incident contract read-only/security assertions.

Frontend coverage includes:

- Security User Administration HTTP mapping;
- Local/OIDC administration surfaces;
- Incident API selection without fallback from API errors to mock data;
- Incident pagination metadata;
- mock-mode `Date` to ISO contract mapping;
- route-level role restrictions.

## Canonical contracts

The applicable physical contracts remain:

```text
documentation/contracts/internal/administration-operational-api-v1.yaml
documentation/contracts/internal/security-user-administration-api-v1.yaml
```

Their lifecycle, approval, generation policy and code-generation authorization
remain governed by:

```text
documentation/contracts/CONTRACT_REGISTRY.yaml
```

LOT 5.8 closure does not promote or alter contract approval/generation status.

## Validation

Focused backend validation:

```bash
cd backend
mvn -pl administration -am test
mvn -pl administration -am clean verify
```

Frontend validation:

```bash
cd frontend
npm run verify:sixpay
```

Canonical repository gates:

```bash
py scripts/verify_master_prompt_input_manifest.py
py scripts/verify_master_engineering_prompt.py
py scripts/verify_baseline.py
```

Optional clean-room proof when required by the delivery gate:

```bash
py scripts/verify_clean_room.py
```

The LOT 5.8 closure status may be declared repository-validated only after the
required commands have actually completed successfully.
