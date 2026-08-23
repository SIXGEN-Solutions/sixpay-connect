export type CustomerStatus = 'ACTIVE' | 'SUSPENDED' | 'CLOSED';

export type SubscriptionStatus =
  | 'PENDING_ACTIVATION'
  | 'ACTIVE'
  | 'SUSPENDED'
  | 'CLOSED';

export interface CustomerBankAccount {
  id: string;
  bankingAccountReference: string;
  accountBindingFingerprint: string;
  maskedAccountIdentifier: string;
  currency: string;
  accountType: string | null;
  defaultAccount: boolean;
  verifiedAt: Date;
}

export interface CustomerMaster {
  id: string;
  financialInstitutionCode: string;
  bankingCustomerReference: string;
  customerNumber: string | null;
  niu: string | null;
  legalName: string;
  email: string | null;
  phoneNumber: string | null;
  status: CustomerStatus;
  statusReason: string | null;
  createdAt: Date;
  updatedAt: Date;
  bankAccounts: CustomerBankAccount[];
}

export interface BankingCustomerPreview {
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
  retrievedAt: Date;
}

export interface CustomerSubscription {
  id: string;
  customerId: string;
  partnerId: string;
  bankAccountId: string;
  status: SubscriptionStatus;
  statusReason: string | null;
  createdAt: Date;
  activatedAt: Date | null;
  updatedAt: Date;
  closedAt: Date | null;
}


export interface CustomerSearchCriteria {
  niu?: string;
  legalName?: string;
  status?: CustomerStatus;
  financialInstitutionCode?: string;
  page: number;
  size: number;
}

export interface CustomerPage {
  content: CustomerMaster[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  first: boolean;
  last: boolean;
}
