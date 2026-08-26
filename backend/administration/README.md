# Administration Module

## Purpose

The Administration module owns administrative HTTP boundaries and operational
queries. It does not own users, identities, roles, permissions or
authentication; those responsibilities belong to Security.

## Responsibilities

- expose Security user-administration commands through an administrative API;
- expose operational overview, settings and integration-health projections;
- search and retrieve operational incidents;
- keep operational concerns separate from Payment audit reporting.

## APIs

Security user administration:

    /internal/api/v1/administration/users

Current operations include create, list, retrieve, update, enable, disable,
delete, local authentication-method management, local password reset, OIDC
identity linking and OIDC identity unlinking.

Operational queries:

    GET /internal/api/v1/administration/overview
    GET /internal/api/v1/administration/settings
    GET /internal/api/v1/administration/integrations

Incident queries:

    GET /internal/api/v1/incidents
    GET /internal/api/v1/incidents/{incidentId}

The active administrative contract is:
documentation/contracts/internal/administration-operational-api-v1.yaml

## Boundaries

- Security remains the owner of canonical users, identities and authorization.
- Reporting remains the owner of immutable Payment audit queries and exports.
- Administration collaborates with other modules through application ports.
- Internal calls remain in-process in the modular monolith.

## Validation

From backend:

    mvn -pl administration -am test
    mvn -pl administration -am clean verify

## Persistence ownership

Administration owns these production tables:

| Table | Purpose |
|---|---|
| operational_incident | Operational incident state |
| operational_incident_timeline | Incident timeline entries |

Security-owned users, identities, credentials and authorization tables are not
duplicated by Administration.

Schema:
backend/administration/src/main/resources/db/migration/V800__administration_baseline.sql
