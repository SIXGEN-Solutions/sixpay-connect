import {
  PaymentDetailResponse,
  PaymentSearchPageResponse,
  PaymentSummaryResponse,
} from '../models/payments.response';
import { PaymentDetail, PaymentSearchPage, PaymentSummary } from '../models/payments';

export function mapPaymentSummaryResponse(response: PaymentSummaryResponse): PaymentSummary {
  return {
    paymentId: response.paymentId,
    paymentReference: response.paymentReference,
    tresorPayRequestId: response.tresorPayRequestId,
    observedCustomerId: response.observedCustomerId ?? null,
    financialInstitutionCode: response.financialInstitutionCode,
    debtorAccount: response.debtorAccount ? { ...response.debtorAccount } : null,
    amount: { ...response.amount },
    status: response.status,
    reasonCode: response.reasonCode ?? null,
    createdAt: new Date(response.createdAt),
    updatedAt: new Date(response.updatedAt),
    finalizedAt: response.finalizedAt ? new Date(response.finalizedAt) : null,
  };
}

export function mapPaymentSearchPageResponse(
  response: PaymentSearchPageResponse,
): PaymentSearchPage {
  return {
    items: response.items.map(mapPaymentSummaryResponse),
    size: response.size,
    hasMore: response.hasMore,
    nextCursor: response.nextCursor ?? null,
    snapshotAt: new Date(response.snapshotAt),
  };
}

export function mapPaymentDetailResponse(response: PaymentDetailResponse): PaymentDetail {
  return {
    ...mapPaymentSummaryResponse(response),
    correlationId: response.correlationId,
    aggregateVersion: response.aggregateVersion,
    bankingVerification: response.bankingVerification
      ? {
          verificationId: response.bankingVerification.verificationId,
          outcome: response.bankingVerification.outcome,
          reasonCodes: [...(response.bankingVerification.reasonCodes ?? [])],
          observedAt: new Date(response.bankingVerification.observedAt),
        }
      : null,
    posting: response.posting
      ? {
          bankPostingReference: response.posting.bankPostingReference ?? null,
          outcome: response.posting.outcome,
          observedAt: response.posting.observedAt ? new Date(response.posting.observedAt) : null,
        }
      : null,
    tfj: response.tfj
      ? {
          status: response.tfj.status,
          businessDate: response.tfj.businessDate ?? null,
          confirmedAt: response.tfj.confirmedAt ? new Date(response.tfj.confirmedAt) : null,
        }
      : null,
    notifications: response.notifications.map((notification) => ({
      type: notification.type,
      status: notification.status,
      eventId: notification.eventId ?? null,
      lastAttemptAt: notification.lastAttemptAt ? new Date(notification.lastAttemptAt) : null,
    })),
    reversal: response.reversal
      ? {
          status: response.reversal.status,
          reversalReference: response.reversal.reversalReference ?? null,
          observedAt: response.reversal.observedAt ? new Date(response.reversal.observedAt) : null,
        }
      : null,
  };
}
