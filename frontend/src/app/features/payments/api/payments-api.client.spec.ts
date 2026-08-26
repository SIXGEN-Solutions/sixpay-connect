import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PaymentsApiClient } from './payments-api.client';

describe('PaymentsApiClient', () => {
  let client: PaymentsApiClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    client = TestBed.inject(PaymentsApiClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('calls the published Payment search endpoint with contract query parameters', () => {
    client.searchPayments({
      paymentReference: 'PAY-2026-0001842',
      financialInstitutionCode: 'LRB',
      status: 'TREASURY_INTEGRATED',
      createdFrom: new Date('2026-08-08T00:00:00Z'),
      amountMin: 1000,
      currency: 'XAF',
      sort: 'CREATED_AT_DESC',
      cursor: 'cursor-2',
      size: 50,
    }).subscribe();

    const request = controller.expectOne((candidate) =>
      candidate.url === '/internal/api/v1/payments',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('paymentReference')).toBe('PAY-2026-0001842');
    expect(request.request.params.get('financialInstitutionCode')).toBe('LRB');
    expect(request.request.params.get('status')).toBe('TREASURY_INTEGRATED');
    expect(request.request.params.get('createdFrom')).toBe('2026-08-08T00:00:00.000Z');
    expect(request.request.params.get('amountMin')).toBe('1000');
    expect(request.request.params.get('currency')).toBe('XAF');
    expect(request.request.params.get('sort')).toBe('CREATED_AT_DESC');
    expect(request.request.params.get('cursor')).toBe('cursor-2');
    expect(request.request.params.get('size')).toBe('50');

    request.flush({
      items: [],
      size: 0,
      hasMore: false,
      snapshotAt: '2026-08-08T14:00:00Z',
    });
  });

  it('encodes paymentId and calls the published detail endpoint', () => {
    client.getPayment('7fa85f64-5717-4562-b3fc-2c963f66afa1').subscribe();

    const request = controller.expectOne(
      '/internal/api/v1/payments/7fa85f64-5717-4562-b3fc-2c963f66afa1',
    );

    expect(request.request.method).toBe('GET');

    request.flush({
      paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afa1',
      paymentReference: 'PAY-2026-0001842',
      tresorPayRequestId: 'TP-2026-440921',
      financialInstitutionCode: 'LRB',
      amount: { amount: 125000, currency: 'XAF' },
      status: 'TREASURY_INTEGRATED',
      createdAt: '2026-08-08T13:47:12Z',
      updatedAt: '2026-08-08T13:47:13Z',
      correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
      aggregateVersion: 8,
      notifications: [],
    });
  });
});
