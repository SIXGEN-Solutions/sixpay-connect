import { AccountingBatchStatus, ReconciliationStatus } from './accounting';

export interface AccountingBatchQuery {
  readonly businessDate?: string;
  readonly status?: AccountingBatchStatus;
  readonly reconciliationStatus?: ReconciliationStatus;
}
