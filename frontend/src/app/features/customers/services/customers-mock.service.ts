import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import {
  ObservedCustomerPaymentsQuery,
  ObservedCustomerSearchQuery,
} from '../models/customer-query';
import {
  ObservedCustomerDetailResponse,
  ObservedCustomerPaymentPageResponse,
  ObservedCustomerPaymentReferenceResponse,
  ObservedCustomerSearchPageResponse,
  ObservedCustomerSummaryResponse,
} from '../models/customers.response';

const CUSTOMER_ONE_ID = '7cb96138-c2b7-4f61-8bb3-b3b00599f101';
const CUSTOMER_TWO_ID = '7cb96138-c2b7-4f61-8bb3-b3b00599f102';

const CUSTOMERS: readonly ObservedCustomerDetailResponse[] = [
  {
    observedCustomerId: CUSTOMER_ONE_ID,
    niu: { maskedValue: 'M********239' },
    legalName: 'CAMEROUN SERVICES SARL',
    phone: { maskedValue: '+237 6** ** ** 21' },
    email: { maskedValue: 'c***@example.cm' },
    institutions: [
      {
        financialInstitutionCode: 'LRB',
        firstObservedAt: '2026-07-13T11:15:00Z',
        lastObservedAt: '2026-08-08T13:47:12Z',
        accounts: [
          { reference: 'ACC-REF-8921', maskedValue: '•••• 8921' },
          { reference: 'ACC-REF-3310', maskedValue: '•••• 3310' },
        ],
      },
    ],
    firstObservedAt: '2026-07-13T11:15:00Z',
    lastObservedAt: '2026-08-08T13:47:12Z',
    totalPayments: 17,
    successfulPayments: 15,
    failedPayments: 2,
    lastPaymentStatus: 'TREASURY_INTEGRATED',
    lastFailureReasonCode: null,
    projectionUpdatedAt: '2026-08-08T13:47:13Z',
    projectionVersion: 31,
    sourceEventWatermark: 'payment-event:0000000000001842',
  },
  {
    observedCustomerId: CUSTOMER_TWO_ID,
    niu: { maskedValue: 'P********821' },
    legalName: 'ETS MBARGA & FILS',
    phone: null,
    email: { maskedValue: 'm***@example.cm' },
    institutions: [
      {
        financialInstitutionCode: 'LRB',
        firstObservedAt: '2026-07-22T08:12:00Z',
        lastObservedAt: '2026-08-08T13:45:03Z',
        accounts: [{ reference: 'ACC-REF-1450', maskedValue: '•••• 1450' }],
      },
    ],
    firstObservedAt: '2026-07-22T08:12:00Z',
    lastObservedAt: '2026-08-08T13:45:03Z',
    totalPayments: 6,
    successfulPayments: 4,
    failedPayments: 2,
    lastPaymentStatus: 'POSTING',
    lastFailureReasonCode: 'BANK_TIMEOUT',
    projectionUpdatedAt: '2026-08-08T13:45:04Z',
    projectionVersion: 12,
    sourceEventWatermark: 'payment-event:0000000000001841',
  },
];

const CUSTOMER_PAYMENTS: Readonly<
  Record<string, readonly ObservedCustomerPaymentReferenceResponse[]>
> = {
  [CUSTOMER_ONE_ID]: [
    {
      paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afa1',
      paymentReference: 'PAY-2026-0001842',
      financialInstitutionCode: 'LRB',
      amount: { amount: 125000, currency: 'XAF' },
      status: 'TREASURY_INTEGRATED',
      reasonCode: null,
      createdAt: '2026-08-08T13:47:12.104Z',
      updatedAt: '2026-08-08T13:47:13.218Z',
    },
    {
      paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afb1',
      paymentReference: 'PAY-2026-0001776',
      financialInstitutionCode: 'LRB',
      amount: { amount: 75000, currency: 'XAF' },
      status: 'TREASURY_INTEGRATED',
      reasonCode: null,
      createdAt: '2026-08-07T20:12:00.000Z',
      updatedAt: '2026-08-07T20:12:01.000Z',
    },
    {
      paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afb2',
      paymentReference: 'PAY-2026-0001704',
      financialInstitutionCode: 'LRB',
      amount: { amount: 35000, currency: 'XAF' },
      status: 'REJECTED',
      reasonCode: 'INSUFFICIENT_FUNDS',
      createdAt: '2026-08-06T10:04:00.000Z',
      updatedAt: '2026-08-06T10:04:00.300Z',
    },
  ],
  [CUSTOMER_TWO_ID]: [
    {
      paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afa2',
      paymentReference: 'PAY-2026-0001841',
      financialInstitutionCode: 'LRB',
      amount: { amount: 62500, currency: 'XAF' },
      status: 'POSTING',
      reasonCode: null,
      createdAt: '2026-08-08T13:45:03.000Z',
      updatedAt: '2026-08-08T13:45:04.100Z',
    },
  ],
};

@Injectable({ providedIn: 'root' })
export class CustomersMockService {
  search(query: ObservedCustomerSearchQuery): Observable<ObservedCustomerSearchPageResponse> {
    let items = CUSTOMERS.filter((customer) => this.matches(customer, query));

    if (query.sort === 'FIRST_OBSERVED_AT_ASC') {
      items = [...items].sort((a, b) => a.firstObservedAt.localeCompare(b.firstObservedAt));
    } else if (query.sort === 'FIRST_OBSERVED_AT_DESC') {
      items = [...items].sort((a, b) => b.firstObservedAt.localeCompare(a.firstObservedAt));
    } else if (query.sort === 'LAST_OBSERVED_AT_ASC') {
      items = [...items].sort((a, b) => a.lastObservedAt.localeCompare(b.lastObservedAt));
    } else {
      items = [...items].sort((a, b) => b.lastObservedAt.localeCompare(a.lastObservedAt));
    }

    const size = Math.max(1, Math.min(query.size ?? 1, 200));
    const offset = query.cursor ? Number(query.cursor) || 0 : 0;
    const pageItems = items.slice(offset, offset + size);
    const nextOffset = offset + pageItems.length;

    return of({
      items: pageItems.map(this.toSummary),
      size: pageItems.length,
      hasMore: nextOffset < items.length,
      nextCursor: nextOffset < items.length ? String(nextOffset) : null,
      snapshotAt: '2026-08-08T14:00:00.000Z',
    });
  }

  get(observedCustomerId: string): Observable<ObservedCustomerDetailResponse | null> {
    return of(
      CUSTOMERS.find((customer) => customer.observedCustomerId === observedCustomerId) ?? null,
    );
  }

  payments(
    observedCustomerId: string,
    query: ObservedCustomerPaymentsQuery,
  ): Observable<ObservedCustomerPaymentPageResponse> {
    let items = [...(CUSTOMER_PAYMENTS[observedCustomerId] ?? [])];

    items = items.filter(
      (payment) =>
        (!query.status || payment.status === query.status) &&
        (!query.createdFrom || new Date(payment.createdAt) >= query.createdFrom) &&
        (!query.createdTo || new Date(payment.createdAt) <= query.createdTo),
    );

    const size = Math.max(1, Math.min(query.size ?? 2, 200));
    const offset = query.cursor ? Number(query.cursor) || 0 : 0;
    const pageItems = items.slice(offset, offset + size);
    const nextOffset = offset + pageItems.length;

    return of({
      items: pageItems,
      size: pageItems.length,
      hasMore: nextOffset < items.length,
      nextCursor: nextOffset < items.length ? String(nextOffset) : null,
      snapshotAt: '2026-08-08T14:00:00.000Z',
    });
  }

  private matches(
    customer: ObservedCustomerDetailResponse,
    query: ObservedCustomerSearchQuery,
  ): boolean {
    const institutionMatches =
      !query.financialInstitutionCode ||
      customer.institutions.some(
        (institution) => institution.financialInstitutionCode === query.financialInstitutionCode,
      );

    return (
      (!query.niu || customer.niu.maskedValue.toLowerCase().includes(query.niu.toLowerCase())) &&
      (!query.legalName ||
        customer.legalName.toLowerCase().includes(query.legalName.toLowerCase())) &&
      institutionMatches &&
      (!query.lastPaymentStatus || customer.lastPaymentStatus === query.lastPaymentStatus) &&
      (!query.lastFailureReasonCode ||
        (customer.lastFailureReasonCode ?? '')
          .toLowerCase()
          .includes(query.lastFailureReasonCode.toLowerCase())) &&
      (!query.firstObservedFrom || new Date(customer.firstObservedAt) >= query.firstObservedFrom) &&
      (!query.firstObservedTo || new Date(customer.firstObservedAt) <= query.firstObservedTo) &&
      (!query.lastObservedFrom || new Date(customer.lastObservedAt) >= query.lastObservedFrom) &&
      (!query.lastObservedTo || new Date(customer.lastObservedAt) <= query.lastObservedTo)
    );
  }

  private readonly toSummary = (
    customer: ObservedCustomerDetailResponse,
  ): ObservedCustomerSummaryResponse => ({
    observedCustomerId: customer.observedCustomerId,
    niu: { ...customer.niu },
    legalName: customer.legalName,
    ...(customer.phone ? { phone: { ...customer.phone } } : { phone: null }),
    ...(customer.email ? { email: { ...customer.email } } : { email: null }),
    firstObservedAt: customer.firstObservedAt,
    lastObservedAt: customer.lastObservedAt,
    totalPayments: customer.totalPayments,
    successfulPayments: customer.successfulPayments,
    failedPayments: customer.failedPayments,
    ...(customer.lastPaymentStatus
      ? { lastPaymentStatus: customer.lastPaymentStatus }
      : { lastPaymentStatus: null }),
    ...(customer.lastFailureReasonCode
      ? { lastFailureReasonCode: customer.lastFailureReasonCode }
      : { lastFailureReasonCode: null }),
    projectionUpdatedAt: customer.projectionUpdatedAt,
    projectionVersion: customer.projectionVersion,
  });
}
