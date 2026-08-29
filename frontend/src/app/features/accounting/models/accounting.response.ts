export interface AccountingBatchSummaryResponse {
  readonly batchId: string;
  readonly businessDate: string;
  readonly financialInstitutionCode: string;
  readonly status: 'COMPLETED' | 'NOT_COMPLETED';
  readonly itemCount: number;
  readonly createdAt: string;
}

export interface AccountingBatchItemResponse {
  readonly paymentId: string;
  readonly publicPaymentReference: string;
  readonly partnerId: string;
  readonly amount: number;
  readonly currency: string;
  readonly paymentOccurredAt: string;
  readonly paymentBusinessDate: string;
  readonly bankPostingReference: string | null;
  readonly tresorPayStatus: string;
  readonly tresorPayStatusCheckedAt: string;
  readonly status: string;
}

export interface AccountingBatchDetailResponse extends AccountingBatchSummaryResponse {
  readonly idempotencyKey: string;
  readonly items: readonly AccountingBatchItemResponse[];
}

export interface AccountingBatchPageResponse {
  readonly content: readonly AccountingBatchSummaryResponse[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}
