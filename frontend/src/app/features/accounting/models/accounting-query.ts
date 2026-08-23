import { AccountingBatchStatus } from './accounting';

export interface AccountingBatchQuery {
  readonly businessDate?: string;
  readonly status?: AccountingBatchStatus;
  readonly page?: number;
  readonly size?: number;
}
