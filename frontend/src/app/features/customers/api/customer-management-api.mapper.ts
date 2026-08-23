import {
  BankingCustomerPreview,
  CustomerMaster,
  CustomerSubscription,
} from '../models/customer-management';
import {
  BankingCustomerPreviewResponse,
  CustomerMasterResponse,
  CustomerSubscriptionResponse,
} from '../models/customer-management.response';

export function mapCustomerMaster(
  response: CustomerMasterResponse,
): CustomerMaster {
  return {
    ...response,
    createdAt: new Date(response.createdAt),
    updatedAt: new Date(response.updatedAt),
    bankAccounts: response.bankAccounts.map((account) => ({
      ...account,
      verifiedAt: new Date(account.verifiedAt),
    })),
  };
}

export function mapBankingPreview(
  response: BankingCustomerPreviewResponse,
): BankingCustomerPreview {
  return {
    ...response,
    retrievedAt: new Date(response.retrievedAt),
  };
}

export function mapCustomerSubscription(
  response: CustomerSubscriptionResponse,
): CustomerSubscription {
  return {
    ...response,
    createdAt: new Date(response.createdAt),
    activatedAt: response.activatedAt ? new Date(response.activatedAt) : null,
    updatedAt: new Date(response.updatedAt),
    closedAt: response.closedAt ? new Date(response.closedAt) : null,
  };
}
