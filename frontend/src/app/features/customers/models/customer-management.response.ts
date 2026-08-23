export interface CustomerBankAccountResponse {
  id: string;
  bankingAccountReference: string;
  accountBindingFingerprint: string;
  maskedAccountIdentifier: string;
  currency: string;
  accountType: string | null;
  defaultAccount: boolean;
  verifiedAt: string;
}

export interface CustomerMasterResponse {
  id: string;
  financialInstitutionCode: string;
  bankingCustomerReference: string;
  customerNumber: string | null;
  niu: string | null;
  legalName: string;
  email: string | null;
  phoneNumber: string | null;
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  statusReason: string | null;
  createdAt: string;
  updatedAt: string;
  bankAccounts: CustomerBankAccountResponse[];
}

export interface BankingCustomerPreviewResponse {
  financialInstitutionCode: string;
  bankingCustomerReference: string;
  customerNumber: string | null;
  niu: string;
  legalName: string;
  email: string | null;
  phoneNumber: string | null;
  accountReference: string;
  maskedAccountIdentifier: string;
  currency: string;
  accountType: string | null;
  retrievedAt: string;
}

export interface CustomerSubscriptionResponse {
  id: string;
  customerId: string;
  partnerId: string;
  bankAccountId: string;
  status: 'PENDING_ACTIVATION' | 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  statusReason: string | null;
  createdAt: string;
  activatedAt: string | null;
  updatedAt: string;
  closedAt: string | null;
}
