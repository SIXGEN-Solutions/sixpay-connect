import {
  ObservedCustomerDetailResponse,
  ObservedCustomerPaymentPageResponse,
  ObservedCustomerPaymentReferenceResponse,
  ObservedCustomerSearchPageResponse,
  ObservedCustomerSummaryResponse,
} from '../models/customers.response';
import {
  ObservedCustomerDetail,
  ObservedCustomerPaymentPage,
  ObservedCustomerPaymentReference,
  ObservedCustomerSearchPage,
  ObservedCustomerSummary,
} from '../models/customers';

export function mapObservedCustomerSummaryResponse(
  response: ObservedCustomerSummaryResponse,
): ObservedCustomerSummary {
  return {
    observedCustomerId: response.observedCustomerId,
    niu: { ...response.niu },
    legalName: response.legalName,
    phone: response.phone ? { ...response.phone } : null,
    email: response.email ? { ...response.email } : null,
    firstObservedAt: new Date(response.firstObservedAt),
    lastObservedAt: new Date(response.lastObservedAt),
    totalPayments: response.totalPayments,
    successfulPayments: response.successfulPayments,
    failedPayments: response.failedPayments,
    lastPaymentStatus: response.lastPaymentStatus ?? null,
    lastFailureReasonCode: response.lastFailureReasonCode ?? null,
    projectionUpdatedAt: new Date(response.projectionUpdatedAt),
    projectionVersion: response.projectionVersion,
  };
}

export function mapObservedCustomerDetailResponse(
  response: ObservedCustomerDetailResponse,
): ObservedCustomerDetail {
  return {
    ...mapObservedCustomerSummaryResponse(response),
    institutions: response.institutions.map((institution) => ({
      financialInstitutionCode: institution.financialInstitutionCode,
      firstObservedAt: new Date(institution.firstObservedAt),
      lastObservedAt: new Date(institution.lastObservedAt),
      accounts: institution.accounts.map((account) => ({ ...account })),
    })),
    sourceEventWatermark: response.sourceEventWatermark,
  };
}

export function mapObservedCustomerSearchPageResponse(
  response: ObservedCustomerSearchPageResponse,
): ObservedCustomerSearchPage {
  return {
    items: response.items.map(mapObservedCustomerSummaryResponse),
    size: response.size,
    hasMore: response.hasMore,
    nextCursor: response.nextCursor ?? null,
    snapshotAt: new Date(response.snapshotAt),
  };
}

export function mapObservedCustomerPaymentReferenceResponse(
  response: ObservedCustomerPaymentReferenceResponse,
): ObservedCustomerPaymentReference {
  return {
    paymentId: response.paymentId,
    paymentReference: response.paymentReference,
    financialInstitutionCode: response.financialInstitutionCode,
    amount: response.amount ? { ...response.amount } : null,
    status: response.status,
    reasonCode: response.reasonCode ?? null,
    createdAt: new Date(response.createdAt),
    updatedAt: new Date(response.updatedAt),
  };
}

export function mapObservedCustomerPaymentPageResponse(
  response: ObservedCustomerPaymentPageResponse,
): ObservedCustomerPaymentPage {
  return {
    items: response.items.map(mapObservedCustomerPaymentReferenceResponse),
    size: response.size,
    hasMore: response.hasMore,
    nextCursor: response.nextCursor ?? null,
    snapshotAt: new Date(response.snapshotAt),
  };
}
