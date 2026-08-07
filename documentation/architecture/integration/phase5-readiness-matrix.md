# Phase 5 readiness matrix

| Capability | Code complete | Deterministic tests | Runbook/ops | External sandbox | Release note |
|---|---|---|---|---|---|
| Integration foundation | YES | YES | YES | N/A | modular-monolith safe |
| TresorPay inbound/callback | YES/contract-dependent | YES | YES | PENDING | external certification required |
| Customer Verification → Amplitude | YES/contract-dependent | YES | YES | PENDING | provider contract still authoritative |
| Payment banking operations | YES/provisional provider contract | YES | YES | PENDING | financial sandbox mandatory before production |
| Observed Customer projection | YES | YES | YES | N/A for first release | in-process outbox is valid |
| Kafka/outbox | YES foundation | YES | YES | topology-dependent | not default internal transport |
| Payment → Accounting | YES/provisional API | YES | YES | PENDING | provider schema/status certification required |
| Notification email | YES | YES | YES | PENDING | production SMTP credentials required |
| Phase 5 deterministic readiness gate | YES | YES | YES | N/A | automated by Lot 5.8 |
| Production readiness | NO CLAIM | — | — | PENDING | explicit go/no-go required |
