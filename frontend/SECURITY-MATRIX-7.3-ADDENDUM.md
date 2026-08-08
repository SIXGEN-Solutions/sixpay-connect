# SIXPAY CONNECT — Phase 7.3 Audit / Reporting RBAC Addendum

The Payment Audit Query API is privileged and classified `RESTRICTED`.

Phase 7.3 maps this frontend capability to the existing `AUDITOR` role only.

| Route | ADMIN | MANAGER | AUDITOR | PARTNER |
|---|:---:|:---:|:---:|:---:|
| `/reporting` | Non | Non | Oui | Non |
| `/reporting/payments/:paymentId/timeline` | Non | Non | Oui | Non |
| `/reporting/audit-records` | Non | Non | Oui | Non |
| `/reporting/audit-records/:auditId` | Non | Non | Oui | Non |
| `/reporting/exports` | Non | Non | Oui | Non |
| `/reporting/exports/:exportId` | Non | Non | Oui | Non |

Backend scopes remain authoritative:

- `payment.audit.read`
- `payment.audit.export`

No assumption is made that `ADMIN` implicitly owns privileged audit scopes.
