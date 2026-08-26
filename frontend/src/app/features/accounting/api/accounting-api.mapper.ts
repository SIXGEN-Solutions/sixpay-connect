import {
  AccountingBatchDetail,
  AccountingBatchItem,
  AccountingBatchSummary,
} from '../models/accounting';
import {
  AccountingBatchDetailResponse,
  AccountingBatchItemResponse,
  AccountingBatchSummaryResponse,
} from '../models/accounting.response';

export function mapAccountingBatchSummaryResponse(
  response: AccountingBatchSummaryResponse,
): AccountingBatchSummary {
  return {
    batchId: response.batchId,
    businessDate: response.businessDate,
    financialInstitutionCode:
      response.financialInstitutionCode,
    status: response.status,
    itemCount: response.itemCount,
    createdAt: new Date(response.createdAt),
  };
}

export function mapAccountingBatchItemResponse(
  response: AccountingBatchItemResponse,
): AccountingBatchItem {
  return {
    paymentId: response.paymentId,
    publicPaymentReference:
      response.publicPaymentReference,
    partnerId: response.partnerId,
    amount: response.amount,
    currency: response.currency,
    paymentOccurredAt:
      new Date(response.paymentOccurredAt),
    paymentBusinessDate:
      response.paymentBusinessDate,
    bankPostingReference:
      response.bankPostingReference,
    tresorPayStatus:
      response.tresorPayStatus,
    tresorPayStatusCheckedAt:
      new Date(response.tresorPayStatusCheckedAt),
    status: response.status,
  };
}

export function mapAccountingBatchDetailResponse(
  response: AccountingBatchDetailResponse,
): AccountingBatchDetail {
  return {
    ...mapAccountingBatchSummaryResponse(response),
    idempotencyKey: response.idempotencyKey,
    items: response.items.map(
      mapAccountingBatchItemResponse,
    ),
  };
}
