import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CustomersApiClient } from './customers-api.client';

describe('CustomersApiClient', () => {
  let client: CustomersApiClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    client = TestBed.inject(CustomersApiClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('calls the published ObservedCustomer search endpoint', () => {
    client
      .search({
        niu: 'M123',
        legalName: 'CAMEROUN SERVICES',
        financialInstitutionCode: 'LRB',
        lastPaymentStatus: 'TREASURY_INTEGRATED',
        lastFailureReasonCode: 'BANK_TIMEOUT',
        firstObservedFrom: new Date('2026-08-01T00:00:00Z'),
        firstObservedTo: new Date('2026-08-08T23:59:59Z'),
        lastObservedFrom: new Date('2026-08-02T00:00:00Z'),
        lastObservedTo: new Date('2026-08-08T23:59:59Z'),
        paymentFrom: new Date('2026-08-01T00:00:00Z'),
        paymentTo: new Date('2026-08-08T23:59:59Z'),
        sort: 'LAST_OBSERVED_AT_DESC',
        cursor: 'cursor-2',
        size: 50,
      })
      .subscribe();

    const request = controller.expectOne(
      (candidate) => candidate.url === '/internal/api/v1/observed-customers',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('niu')).toBe('M123');
    expect(request.request.params.get('legalName')).toBe('CAMEROUN SERVICES');
    expect(request.request.params.get('financialInstitutionCode')).toBe('LRB');
    expect(request.request.params.get('lastPaymentStatus')).toBe('TREASURY_INTEGRATED');
    expect(request.request.params.get('lastFailureReasonCode')).toBe('BANK_TIMEOUT');
    expect(request.request.params.get('firstObservedFrom')).toBe('2026-08-01T00:00:00.000Z');
    expect(request.request.params.get('paymentTo')).toBe('2026-08-08T23:59:59.000Z');
    expect(request.request.params.get('sort')).toBe('LAST_OBSERVED_AT_DESC');
    expect(request.request.params.get('cursor')).toBe('cursor-2');
    expect(request.request.params.get('size')).toBe('50');

    request.flush({
      items: [],
      size: 0,
      hasMore: false,
      snapshotAt: '2026-08-08T14:00:00Z',
    });
  });

  it('calls the published ObservedCustomer detail endpoint', () => {
    client.get('7cb96138-c2b7-4f61-8bb3-b3b00599f101').subscribe();

    const request = controller.expectOne(
      '/internal/api/v1/observed-customers/7cb96138-c2b7-4f61-8bb3-b3b00599f101',
    );

    expect(request.request.method).toBe('GET');

    request.flush({
      observedCustomerId: '7cb96138-c2b7-4f61-8bb3-b3b00599f101',
      niu: { maskedValue: 'M********239' },
      legalName: 'CAMEROUN SERVICES SARL',
      firstObservedAt: '2026-07-13T11:15:00Z',
      lastObservedAt: '2026-08-08T13:47:12Z',
      totalPayments: 17,
      successfulPayments: 15,
      failedPayments: 2,
      lastPaymentStatus: 'TREASURY_INTEGRATED',
      projectionUpdatedAt: '2026-08-08T13:47:13Z',
      projectionVersion: 31,
      institutions: [],
      sourceEventWatermark: 'payment-event:0000000000001842',
    });
  });

  it('calls the published linked Payments endpoint with cursor parameters', () => {
    client
      .payments('7cb96138-c2b7-4f61-8bb3-b3b00599f101', {
        status: 'TREASURY_INTEGRATED',
        createdFrom: new Date('2026-08-01T00:00:00Z'),
        createdTo: new Date('2026-08-08T23:59:59Z'),
        cursor: 'cursor-payments-2',
        size: 25,
      })
      .subscribe();

    const request = controller.expectOne(
      (candidate) =>
        candidate.url ===
        '/internal/api/v1/observed-customers/7cb96138-c2b7-4f61-8bb3-b3b00599f101/payments',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('status')).toBe('TREASURY_INTEGRATED');
    expect(request.request.params.get('createdFrom')).toBe('2026-08-01T00:00:00.000Z');
    expect(request.request.params.get('cursor')).toBe('cursor-payments-2');
    expect(request.request.params.get('size')).toBe('25');

    request.flush({
      items: [],
      size: 0,
      hasMore: false,
      snapshotAt: '2026-08-08T14:00:00Z',
    });
  });
});
