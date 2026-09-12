export type AccountingT1OperationalCandidateStatus =
  | 'AWAITING_TRESORPAY_VERIFICATION'
  | 'ELIGIBLE_FOR_BATCH'
  | 'INELIGIBLE_FOR_CURRENT_SELECTION'
  | 'ASSIGNED_TO_BATCH';

export type AccountingT1EligibilityReason =
  | 'NONE'
  | 'TRESORPAY_STATUS_NOT_COMPLETED'
  | 'TRESORPAY_STATUS_UNAVAILABLE'
  | 'OUTSIDE_SELECTION_WINDOW';

export type AccountingT1TechnicalIssue =
  | 'NONE'
  | 'TRESORPAY_STATUS_LOOKUP_UNAVAILABLE'
  | 'ACCOUNTING_SUBMISSION_OUTCOME_UNKNOWN'
  | 'ACCOUNTING_RECONCILIATION_PENDING';

export interface AccountingT1OperationalResponse {
  candidateId: string;
  paymentId: string;
  publicPaymentReference: string;
  financialInstitutionCode: string;
  accountingBusinessDate: string;
  status: AccountingT1OperationalCandidateStatus;
  tresorPayProviderStatus: string | null;
  tresorPayCheckedAt: string | null;
  eligibilityReason: AccountingT1EligibilityReason;
  selectionBusinessDate: string;
  selectionFromInclusive: string;
  selectionToExclusive: string;
  technicalIssue: AccountingT1TechnicalIssue;
  batchId: string | null;
}

export interface AccountingT1OperationalPageResponse {
  content: AccountingT1OperationalResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AccountingT1OperationalQuery {
  businessDate?: string;
  status?: AccountingT1OperationalCandidateStatus;
  paymentReference?: string;
  page?: number;
  size?: number;
}
