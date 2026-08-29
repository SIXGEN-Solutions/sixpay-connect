import { PaymentStatus } from './payments';

export type PaymentSort =
  'CREATED_AT_ASC' | 'CREATED_AT_DESC' | 'UPDATED_AT_ASC' | 'UPDATED_AT_DESC';

export interface PaymentSearchQuery {
  readonly paymentReference?: string;
  readonly tresorPayRequestId?: string;
  readonly observedCustomerId?: string;
  readonly financialInstitutionCode?: string;
  readonly status?: PaymentStatus;
  readonly reasonCode?: string;
  readonly createdFrom?: Date;
  readonly createdTo?: Date;
  readonly amountMin?: number;
  readonly amountMax?: number;
  readonly currency?: string;
  readonly sort?: PaymentSort;
  readonly cursor?: string;
  readonly size?: number;
}
