export const PAYMENT_STATUSES = [
  'RECEIVED',
  'PENDING_CONFIRMATION',
  'AUTHORIZATION_CHECKING',
  'BANKING_VERIFICATION_PENDING',
  'FUNDS_CONTROL_PENDING',
  'TREASURY_ACCOUNT_RESOLUTION_PENDING',
  'APPROVED_FOR_POSTING',
  'POSTING_PENDING',
  'POSTING_OUTCOME_UNKNOWN',
  'DEBIT_CONFIRMED',
  'POSTED_PENDING_TFJ',
  'REVERSAL_REQUIRED',
  'REVERSAL_PENDING',
  'REVERSAL_OUTCOME_UNKNOWN',
  'REJECTED',
  'FAILED',
  'TREASURY_INTEGRATED',
  'REVERSED',
] as const;

export type PaymentStatusResponse = (typeof PAYMENT_STATUSES)[number];

export interface MoneyResponse {
  readonly amount: number;
  readonly currency: string;
}

export interface MaskedAccountReferenceResponse {
  readonly reference: string;
  readonly maskedValue: string;
}

export interface PaymentSummaryResponse {
  readonly paymentId: string;
  readonly paymentReference: string;
  readonly tresorPayRequestId: string;
  readonly observedCustomerId?: string | null;
  readonly financialInstitutionCode: string;
  readonly debtorAccount?: MaskedAccountReferenceResponse;
  readonly amount: MoneyResponse;
  readonly status: PaymentStatusResponse;
  readonly reasonCode?: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly finalizedAt?: string | null;
}

export interface PaymentSearchPageResponse {
  readonly items: readonly PaymentSummaryResponse[];
  readonly size: number;
  readonly hasMore: boolean;
  readonly nextCursor?: string | null;
  readonly snapshotAt: string;
}

export interface BankingVerificationSummaryResponse {
  readonly verificationId: string;
  readonly outcome: 'VERIFIED' | 'REJECTED' | 'DEFERRED' | 'FAILED';
  readonly reasonCodes?: readonly string[];
  readonly observedAt: string;
}

export interface PostingSummaryResponse {
  readonly bankPostingReference?: string | null;
  readonly outcome:
    | 'NOT_REQUESTED'
    | 'PENDING'
    | 'UNKNOWN'
    | 'DEBIT_CONFIRMED'
    | 'CUT_CREDIT_CONFIRMED'
    | 'FAILED'
    | 'PARTIAL';
  readonly observedAt?: string | null;
}

export interface TfjSummaryResponse {
  readonly status: 'NOT_APPLICABLE' | 'PENDING' | 'INTEGRATED' | 'FAILED' | 'QUARANTINED';
  readonly businessDate?: string | null;
  readonly confirmedAt?: string | null;
}

export interface NotificationSummaryResponse {
  readonly type: 'IMMEDIATE' | 'TREASURY_FINAL' | 'REVERSAL';
  readonly status: 'NOT_REQUESTED' | 'PENDING' | 'DELIVERED' | 'FAILED' | 'DLQ';
  readonly eventId?: string | null;
  readonly lastAttemptAt?: string | null;
}

export interface ReversalSummaryResponse {
  readonly status: 'NOT_REQUIRED' | 'REQUIRED' | 'PENDING' | 'UNKNOWN' | 'REVERSED' | 'FAILED';
  readonly reversalReference?: string | null;
  readonly observedAt?: string | null;
}

export interface PaymentDetailResponse extends PaymentSummaryResponse {
  readonly correlationId: string;
  readonly aggregateVersion: number;
  readonly bankingVerification?: BankingVerificationSummaryResponse | null;
  readonly posting?: PostingSummaryResponse | null;
  readonly tfj?: TfjSummaryResponse | null;
  readonly notifications: readonly NotificationSummaryResponse[];
  readonly reversal?: ReversalSummaryResponse | null;
}
