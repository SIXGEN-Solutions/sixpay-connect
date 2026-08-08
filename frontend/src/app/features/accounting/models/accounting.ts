export type AccountingBatchStatus =
  | 'READY'
  | 'SUBMITTED'
  | 'ACCEPTED'
  | 'RECONCILING'
  | 'COMPLETED'
  | 'FAILED';

export type ReconciliationStatus = 'MATCHED' | 'PARTIAL_MATCH' | 'UNMATCHED';

export type AccountingDiscrepancyType =
  | 'MISSING_PROVIDER_ITEM'
  | 'AMOUNT_MISMATCH'
  | 'STATUS_MISMATCH'
  | 'DUPLICATE_PROVIDER_ITEM'
  | 'UNKNOWN_PROVIDER_ITEM';

export interface AccountingBatchSummary {
  readonly batchId: string;
  readonly businessDate: string;
  readonly windowLabel: string;
  readonly itemCount: number;
  readonly reconciledCount: number;
  readonly discrepancyCount: number;
  readonly status: AccountingBatchStatus;
  readonly reconciliationStatus: ReconciliationStatus;
  readonly submittedAt: Date | null;
  readonly updatedAt: Date;
}

export interface AccountingDiscrepancy {
  readonly discrepancyId: string;
  readonly paymentId: string;
  readonly paymentReference: string;
  readonly type: AccountingDiscrepancyType;
  readonly expectedAmount: number | null;
  readonly observedAmount: number | null;
  readonly currency: string;
  readonly reasonCode: string;
  readonly detectedAt: Date;
}

export interface AccountingBatchDetail extends AccountingBatchSummary {
  readonly submissionStatus: 'NOT_SUBMITTED' | 'ACCEPTED' | 'REJECTED';
  readonly providerReference: string | null;
  readonly lastReconciledAt: Date | null;
  readonly discrepancies: readonly AccountingDiscrepancy[];
}
