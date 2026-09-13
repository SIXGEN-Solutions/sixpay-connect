export interface AccountingT1ManualExecutionResponse {
  readonly batchId: string;
  readonly businessDate: string;
  readonly financialInstitutionCode: string;
  readonly batchStatus: 'COMPLETED' | 'NOT_COMPLETED';
  readonly submissionState:
    | 'READY'
    | 'SUBMITTING'
    | 'SUBMITTED'
    | 'OUTCOME_UNKNOWN'
    | 'RECONCILIATION_REQUIRED'
    | 'COMPLETED'
    | 'REJECTED';
  readonly providerBatchReference: string | null;
}
