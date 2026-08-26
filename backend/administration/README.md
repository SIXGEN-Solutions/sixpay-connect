## Persistence ownership

Administration owns the following production tables:

| Table | Purpose |
|---|---|
| operational_incident | Operational incident state and searchable attributes |
| operational_incident_timeline | Incident timeline entries |

Security-owned users, identities, credentials and authorization tables remain
owned by the security module and are not duplicated here.

The schema is maintained by:
backend/administration/src/main/resources/db/migration/V800__administration_baseline.sql
