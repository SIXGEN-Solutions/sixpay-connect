import {
  mapObservedCustomerDetailResponse,
  mapObservedCustomerPaymentPageResponse,
  mapObservedCustomerSearchPageResponse,
} from './customers-api.mapper';
import {
  ObservedCustomerDetailResponse,
  ObservedCustomerPaymentPageResponse,
  ObservedCustomerSearchPageResponse,
} from '../models/customers.response';

describe('customers-api.mapper', () => {
  const detail: ObservedCustomerDetailResponse = {
    observedCustomerId: '7cb96138-c2b7-4f61-8bb3-b3b00599f101',
    niu: { maskedValue: 'M********239' },
    legalName: 'CAMEROUN SERVICES SARL',
    phone: null,
    email: { maskedValue: 'c***@example.cm' },
    institutions: [
      {
        financialInstitutionCode: 'LRB',
        firstObservedAt: '2026-07-13T11:15:00Z',
        lastObservedAt: '2026-08-08T13:47:12Z',
        accounts: [{ reference: 'ACC-REF-8921', maskedValue: '•••• 8921' }],
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
  };

  it('maps detail dates and nested institutions', () => {
    const mapped = mapObservedCustomerDetailResponse(detail);

    expect(mapped.firstObservedAt).toBeInstanceOf(Date);
    expect(mapped.projectionUpdatedAt).toBeInstanceOf(Date);
    expect(mapped.institutions).toHaveLength(1);
    expect(mapped.institutions[0]?.firstObservedAt).toBeInstanceOf(Date);
    expect(mapped.phone).toBeNull();
  });

  it('maps search page metadata', () => {
    const page: ObservedCustomerSearchPageResponse = {
      items: [detail],
      size: 1,
      hasMore: false,
      snapshotAt: '2026-08-08T14:00:00Z',
    };

    const mapped = mapObservedCustomerSearchPageResponse(page);

    expect(mapped.items).toHaveLength(1);
    expect(mapped.snapshotAt).toBeInstanceOf(Date);
  });

  it('maps linked Payment references with UUIDs', () => {
    const page: ObservedCustomerPaymentPageResponse = {
      items: [
        {
          paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afa1',
          paymentReference: 'PAY-2026-0001842',
          financialInstitutionCode: 'LRB',
          amount: { amount: 125000, currency: 'XAF' },
          status: 'TREASURY_INTEGRATED',
          reasonCode: null,
          createdAt: '2026-08-08T13:47:12Z',
          updatedAt: '2026-08-08T13:47:13Z',
        },
      ],
      size: 1,
      hasMore: false,
      snapshotAt: '2026-08-08T14:00:00Z',
    };

    const mapped = mapObservedCustomerPaymentPageResponse(page);

    expect(mapped.items).toHaveLength(1);
    expect(mapped.items[0]?.createdAt).toBeInstanceOf(Date);
    expect(mapped.items[0]?.paymentId).toBe('7fa85f64-5717-4562-b3fc-2c963f66afa1');
  });
});
