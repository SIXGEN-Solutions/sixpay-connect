# DA-10 audit constraint remediation

## Problem

The Java enum `SecurityAuditEventType` already contains `PASSWORD_CHANGED`
and the CRUD-related audit values, while the original database check constraint
created by `V202608110400__security_operational_audit.sql` still contains the
older subset.

A successful LOCAL password change therefore fails at transaction commit when
the application attempts to append the `PASSWORD_CHANGED` security audit event.

## Fix

Add a new Flyway migration. Do not edit the already-applied historical
`V202608110400__security_operational_audit.sql`.

The new migration replaces only the check constraint and synchronizes it with
the complete current `SecurityAuditEventType` enum.

## Validation

From `backend/`:

```bash
mvn clean package
```

Start the application and verify that Flyway applies the new migration.

In PostgreSQL:

```sql
SELECT pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conname = 'ck_security_audit_event_type';
```

Then retry the forced password-change flow. Expected result:

```text
POST /api/v1/auth/password/change
-> 204 No Content
-> PASSWORD_CHANGED inserted in security_audit_events
-> /api/v1/auth/me returns passwordChangeRequired=false
```
