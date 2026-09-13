# Administration Module

## Purpose

Administration owns administrative HTTP boundaries, operational queries,
dynamic runtime-setting management and operational incident visibility.
Security owns users, identities, roles, permissions and authentication.

## Responsibilities

- expose Security user-administration commands;
- expose operational overview, settings and integration-health projections;
- manage approved dynamic settings with versioned history and rollback;
- search and retrieve operational incidents;
- keep operational concerns separate from Payment audit reporting.

## APIs

Security user administration:

```text
/internal/api/v1/administration/users
```

Operational queries:

```text
GET /internal/api/v1/administration/overview
GET /internal/api/v1/administration/settings
GET /internal/api/v1/administration/integrations
```

Dynamic settings:

```text
GET  /internal/api/v1/administration/dynamic-settings
GET  /internal/api/v1/administration/dynamic-settings/{key}
PUT  /internal/api/v1/administration/dynamic-settings/{key}
GET  /internal/api/v1/administration/dynamic-settings/{key}/history
POST /internal/api/v1/administration/dynamic-settings/{key}/rollback
```

Incident queries:

```text
GET /internal/api/v1/incidents
GET /internal/api/v1/incidents/{incidentId}
```

## Boundaries

- Security owns canonical users, identities and authorization.
- Reporting owns immutable Payment audit queries and exports.
- Administration owns operational incidents and dynamic-setting persistence.
- Cross-module collaboration uses application ports.

## Validation

```bash
mvn -pl administration -am test
mvn -pl administration -am clean verify
```

## Persistence ownership

| Table | Purpose |
|---|---|
| `operational_incident` | Operational incident state |
| `operational_incident_timeline` | Incident timeline entries |
| `general_parameter` | General parameter values |
| `dynamic_setting_value` | Current dynamic-setting values |
| `dynamic_setting_history` | Versioned dynamic-setting history |

Security-owned persistence is not duplicated by Administration.

## Database baseline

Current Flyway baseline:

```text
V800__administration_baseline.sql
```

