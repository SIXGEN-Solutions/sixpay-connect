export type TfjOperationalCategory =
  | 'MATCHED'
  | 'QUARANTINED_UNMATCHED'
  | 'QUARANTINED_AMBIGUOUS'
  | 'FAILED'
  | 'FINALITY_PUBLICATION_PENDING'
  | 'COMPLETED';

export type TfjStatus = 'PENDING' | 'INTEGRATED' | 'FAILED';

export type TfjObservationChannel = 'ASYNC_CALLBACK' | 'SCHEDULED_LOOKUP';

export type TfjMatchStatus = 'MATCHED' | 'UNMATCHED' | 'AMBIGUOUS';

export type TfjRecoveryAction = 'MANUAL_RECONCILIATION' | 'REVERSAL_REVIEW' | 'REVERSAL_REQUIRED';

export interface TfjOperationalQuery {
  businessDate?: string;
  category?: TfjOperationalCategory;
  paymentReference?: string;
  bankPostingReference?: string;
  page?: number;
  size?: number;
}

export interface TfjOperationalResponse {
  confirmationId: string;
  financialInstitutionCode: string;
  businessDate: string;
  paymentReference: string;
  bankPostingReference: string;
  tfjBatchReference: string | null;
  tfjStatus: TfjStatus;
  confirmedAt: string;
  observationChannel: TfjObservationChannel;
  correlationId: string;
  matchStatus: TfjMatchStatus;
  matchedPaymentId: string | null;
  finalityPublishedAt: string | null;
  failureCode: string | null;
  recoveryAction: TfjRecoveryAction | null;
  category: TfjOperationalCategory;
}

export interface TfjOperationalPageResponse {
  content: TfjOperationalResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
