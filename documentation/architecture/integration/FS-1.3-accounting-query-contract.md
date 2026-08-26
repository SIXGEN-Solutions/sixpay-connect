# FS-1.3 — Accounting Query contract

Status: **DONE**

Validated internal endpoints:

```text
GET /internal/api/v1/accounting-batches
GET /internal/api/v1/accounting-batches/{batchId}
```

Published contract:

```text
documentation/contracts/internal/accounting-query-api-v1.yaml
```

Implementation:

```text
HTTP
 -> AccountingBatchQueryController
 -> AccountingBatchQueryUseCase
 -> AccountingBatchQueryService
 -> AccountingBatchQueryPort
 -> AccountingBatchQueryAdapter
 -> AccountingBatchSpringDataRepository
 -> PostgreSQL
```

AccountingService Angular remains unchanged in FS-1.3.
