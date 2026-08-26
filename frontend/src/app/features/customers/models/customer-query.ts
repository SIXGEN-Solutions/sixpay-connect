import { ObservedCustomerPaymentStatus } from './customers';

export type ObservedCustomerSort =
  | 'FIRST_OBSERVED_AT_ASC'
  | 'FIRST_OBSERVED_AT_DESC'
  | 'LAST_OBSERVED_AT_ASC'
  | 'LAST_OBSERVED_AT_DESC';

export interface ObservedCustomerSearchQuery {
  readonly niu?: string;
  readonly legalName?: string;
  readonly financialInstitutionCode?: string;
  readonly lastPaymentStatus?: ObservedCustomerPaymentStatus;
  readonly lastFailureReasonCode?: string;
  readonly firstObservedFrom?: Date;
  readonly firstObservedTo?: Date;
  readonly lastObservedFrom?: Date;
  readonly lastObservedTo?: Date;
  readonly paymentFrom?: Date;
  readonly paymentTo?: Date;
  readonly sort?: ObservedCustomerSort;
  readonly cursor?: string;
  readonly size?: number;
}

export interface ObservedCustomerPaymentsQuery {
  readonly status?: ObservedCustomerPaymentStatus;
  readonly createdFrom?: Date;
  readonly createdTo?: Date;
  readonly cursor?: string;
  readonly size?: number;
}
