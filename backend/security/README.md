## Persistence ownership

Security owns the following production tables:

| Table/family | Purpose |
|---|---|
| security_user_accounts | Canonical SIXPAY user accounts |
| security_user_identities | Local and external identity links |
| security_local_users | Local authentication credentials and state |
| security_user_roles | User role assignments |
| security_user_permissions | User permission assignments |
| security_password_history | Password history and reuse protection |
| security_authentication_audit | Authentication attempts and outcomes |
| security_audit_events | Security and authorization audit events |

Administration exposes HTTP management boundaries but does not own or duplicate
these tables.

The schema is maintained by:
backend/security/src/main/resources/db/migration/V700__security_baseline.sql
