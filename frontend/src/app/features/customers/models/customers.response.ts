export const OBSERVED_CUSTOMER_PAYMENT_STATUSES = [
  'RECEIVED',
  'AUTHORIZATION_CHECKING',
  'BANKING_CHECKING',
  'REJECTED',
  'APPROVED',
  'POSTING',
  'ACCOUNTING_OUTCOME_UNKNOWN',
  'DEBITED',
  'CUT_CREDITED',
  'REVERSAL_REQUIRED',
  'REVERSAL_PENDING',
  'REVERSED',
  'FAILED',
  'NOTIFIED',
  'PENDING_END_OF_DAY_CONFIRMATION',
  'TREASURY_INTEGRATED',
] as const;

export type ObservedCustomerPaymentStatusResponse =
  (typeof OBSERVED_CUSTOMER_PAYMENT_STATUSES)[number];

export interface MaskedIdentifierResponse {
  readonly maskedValue: string;
}

export interface MaskedAccountReferenceResponse {
  readonly reference: string;
  readonly maskedValue: string;
}

export interface InstitutionObservationResponse {
  readonly financialInstitutionCode: string;
  readonly firstObservedAt: string;
  readonly lastObservedAt: string;
  readonly accounts: readonly MaskedAccountReferenceResponse[];
}

export interface ObservedCustomerSummaryResponse {
  readonly observedCustomerId: string;
  readonly niu: MaskedIdentifierResponse;
  readonly legalName: string;
  readonly phone?: MaskedIdentifierResponse | null;
  readonly email?: MaskedIdentifierResponse | null;
  readonly firstObservedAt: string;
  readonly lastObservedAt: string;
  readonly totalPayments: number;
  readonly successfulPayments: number;
  readonly failedPayments: number;
  readonly lastPaymentStatus?: ObservedCustomerPaymentStatusResponse | null;
  readonly lastFailureReasonCode?: string | null;
  readonly projectionUpdatedAt: string;
  readonly projectionVersion: number;
}

export interface ObservedCustomerDetailResponse extends ObservedCustomerSummaryResponse {
  readonly institutions: readonly InstitutionObservationResponse[];
  readonly sourceEventWatermark: string;
}

export interface ObservedCustomerSearchPageResponse {
  readonly items: readonly ObservedCustomerSummaryResponse[];
  readonly size: number;
  readonly hasMore: boolean;
  readonly nextCursor?: string | null;
  readonly snapshotAt: string;
}

export interface ObservedCustomerPaymentReferenceResponse {
  readonly paymentId: string;
  readonly paymentReference: string;
  readonly financialInstitutionCode: string;
  readonly amount?: {
    readonly amount: number;
    readonly currency: string;
  };
  readonly status: ObservedCustomerPaymentStatusResponse;
  readonly reasonCode?: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ObservedCustomerPaymentPageResponse {
  readonly items: readonly ObservedCustomerPaymentReferenceResponse[];
  readonly size: number;
  readonly hasMore: boolean;
  readonly nextCursor?: string | null;
  readonly snapshotAt: string;
}
