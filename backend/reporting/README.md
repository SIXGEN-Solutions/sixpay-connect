## Persistence ownership

Reporting owns the following production tables:

| Table | Purpose |
|---|---|
| reporting_payment_audit_evidence | Read-side Payment audit evidence |
| reporting_payment_audit_export_job | Controlled audit export jobs |

Reporting reads Payment audit evidence for query and export purposes. It does
not update Payment-owned tables or persist financial state.

The schema is maintained by:
backend/reporting/src/main/resources/db/migration/V500__reporting_baseline.sql
