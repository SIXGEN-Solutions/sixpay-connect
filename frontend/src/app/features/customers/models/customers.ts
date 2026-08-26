import { ObservedCustomerPaymentStatusResponse } from './customers.response';

export type ObservedCustomerPaymentStatus = ObservedCustomerPaymentStatusResponse;

export interface MaskedIdentifier {
  readonly maskedValue: string;
}

export interface MaskedAccountReference {
  readonly reference: string;
  readonly maskedValue: string;
}

export interface InstitutionObservation {
  readonly financialInstitutionCode: string;
  readonly firstObservedAt: Date;
  readonly lastObservedAt: Date;
  readonly accounts: readonly MaskedAccountReference[];
}

export interface ObservedCustomerSummary {
  readonly observedCustomerId: string;
  readonly niu: MaskedIdentifier;
  readonly legalName: string;
  readonly phone: MaskedIdentifier | null;
  readonly email: MaskedIdentifier | null;
  readonly firstObservedAt: Date;
  readonly lastObservedAt: Date;
  readonly totalPayments: number;
  readonly successfulPayments: number;
  readonly failedPayments: number;
  readonly lastPaymentStatus: ObservedCustomerPaymentStatus | null;
  readonly lastFailureReasonCode: string | null;
  readonly projectionUpdatedAt: Date;
  readonly projectionVersion: number;
}

export interface ObservedCustomerDetail extends ObservedCustomerSummary {
  readonly institutions: readonly InstitutionObservation[];
  readonly sourceEventWatermark: string;
}

export interface ObservedCustomerSearchPage {
  readonly items: readonly ObservedCustomerSummary[];
  readonly size: number;
  readonly hasMore: boolean;
  readonly nextCursor: string | null;
  readonly snapshotAt: Date;
}

export interface ObservedCustomerPaymentReference {
  readonly paymentId: string;
  readonly paymentReference: string;
  readonly financialInstitutionCode: string;
  readonly amount: { readonly amount: number; readonly currency: string } | null;
  readonly status: ObservedCustomerPaymentStatus;
  readonly reasonCode: string | null;
  readonly createdAt: Date;
  readonly updatedAt: Date;
}

export interface ObservedCustomerPaymentPage {
  readonly items: readonly ObservedCustomerPaymentReference[];
  readonly size: number;
  readonly hasMore: boolean;
  readonly nextCursor: string | null;
  readonly snapshotAt: Date;
}
