export interface BankingCustomerPreviewRequest {
  financialInstitutionCode: string;
  niu?: string;
  customerNumber?: string;
  accountReference: string;
}

export interface EnrollCustomerRequest {
  financialInstitutionCode: string;
  niu?: string;
  customerNumber?: string;
  accountReference: string;
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
  bankAccountId: string;
}
