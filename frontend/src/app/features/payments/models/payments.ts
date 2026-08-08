import { PaymentStatusResponse } from './payments.response';

export type PaymentStatus = PaymentStatusResponse;

export interface Money {
  readonly amount: number;
  readonly currency: string;
}

export interface MaskedAccountReference {
  readonly reference: string;
  readonly maskedValue: string;
}

export interface PaymentSummary {
  readonly paymentId: string;
  readonly paymentReference: string;
  readonly tresorPayRequestId: string;
  readonly observedCustomerId: string | null;
  readonly financialInstitutionCode: string;
  readonly debtorAccount: MaskedAccountReference | null;
  readonly amount: Money;
  readonly status: PaymentStatus;
  readonly reasonCode: string | null;
  readonly createdAt: Date;
  readonly updatedAt: Date;
  readonly finalizedAt: Date | null;
}

export interface PaymentSearchPage {
  readonly items: readonly PaymentSummary[];
  readonly size: number;
  readonly hasMore: boolean;
  readonly nextCursor: string | null;
  readonly snapshotAt: Date;
}

export interface BankingVerificationSummary {
  readonly verificationId: string;
  readonly outcome: 'VERIFIED' | 'REJECTED' | 'DEFERRED' | 'FAILED';
  readonly reasonCodes: readonly string[];
  readonly observedAt: Date;
}

export interface PostingSummary {
  readonly bankPostingReference: string | null;
  readonly outcome:
    | 'NOT_REQUESTED'
    | 'PENDING'
    | 'UNKNOWN'
    | 'DEBIT_CONFIRMED'
    | 'CUT_CREDIT_CONFIRMED'
    | 'FAILED'
    | 'PARTIAL';
  readonly observedAt: Date | null;
}

export interface TfjSummary {
  readonly status: 'NOT_APPLICABLE' | 'PENDING' | 'INTEGRATED' | 'FAILED' | 'QUARANTINED';
  readonly businessDate: string | null;
  readonly confirmedAt: Date | null;
}

export interface NotificationSummary {
  readonly type: 'IMMEDIATE' | 'TREASURY_FINAL' | 'REVERSAL';
  readonly status: 'NOT_REQUESTED' | 'PENDING' | 'DELIVERED' | 'FAILED' | 'DLQ';
  readonly eventId: string | null;
  readonly lastAttemptAt: Date | null;
}

export interface ReversalSummary {
  readonly status: 'NOT_REQUIRED' | 'REQUIRED' | 'PENDING' | 'UNKNOWN' | 'REVERSED' | 'FAILED';
  readonly reversalReference: string | null;
  readonly observedAt: Date | null;
}

export interface PaymentDetail extends PaymentSummary {
  readonly correlationId: string;
  readonly aggregateVersion: number;
  readonly bankingVerification: BankingVerificationSummary | null;
  readonly posting: PostingSummary | null;
  readonly tfj: TfjSummary | null;
  readonly notifications: readonly NotificationSummary[];
  readonly reversal: ReversalSummary | null;
}
