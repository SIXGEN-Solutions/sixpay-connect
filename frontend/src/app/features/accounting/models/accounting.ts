export type AccountingBatchStatus = 'COMPLETED' | 'NOT_COMPLETED';

export interface AccountingBatchSummary {
  readonly batchId: string;
  readonly businessDate: string;
  readonly financialInstitutionCode: string;
  readonly status: AccountingBatchStatus;
  readonly itemCount: number;
  readonly createdAt: Date;
}

export interface AccountingBatchItem {
  readonly paymentId: string;
  readonly publicPaymentReference: string;
  readonly partnerId: string;
  readonly amount: number;
  readonly currency: string;
  readonly paymentOccurredAt: Date;
  readonly paymentBusinessDate: string;
  readonly bankPostingReference: string | null;
  readonly tresorPayStatus: string;
  readonly tresorPayStatusCheckedAt: Date;
  readonly status: string;
}

export interface AccountingBatchDetail extends AccountingBatchSummary {
  readonly idempotencyKey: string;
  readonly items: readonly AccountingBatchItem[];
}
