export interface BankingCustomerPreviewRequest {
  financialInstitutionCode: string;
  niu?: string;
  customerNumber: string;
}

export interface EnrollCustomerRequest {
  financialInstitutionCode: string;
  customerNumber: string;
  /** Bank-selected account, never a manually entered reference. */
  accountReference: string;
  niu?: string;
}

export interface UpdateCustomerRequest {
  legalName: string;
  email: string | null;
  phoneNumber: string | null;
}

export interface ReasonRequest {
  reason: string;
}

export interface AddBankAccountRequest {
  accountReference: string;
}

export interface CreateSubscriptionRequest {
  customerId: string;
  partnerId: string;
  /** Optional; backend resolves the customer's verified default account. */
  bankAccountId?: string;
}
