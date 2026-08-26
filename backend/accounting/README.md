## Persistence ownership

Accounting owns the following production tables:

| Table | Purpose |
|---|---|
| accounting_batches | Accounting batch identity, status and submission state |
| accounting_batch_items | Payment items assigned to a batch |
| accounting_batch_tracking | Batch submission and reconciliation tracking |
| accounting_batch_item_tracking | Item-level submission and reconciliation tracking |

The schema is maintained by the module migration:
backend/accounting/src/main/resources/db/migration/V400__accounting_baseline.sql
