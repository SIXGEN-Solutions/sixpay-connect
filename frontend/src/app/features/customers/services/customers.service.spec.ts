import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { CustomersApiClient } from '../api/customers-api.client';
import {
  ObservedCustomerDetailResponse,
  ObservedCustomerPaymentPageResponse,
  ObservedCustomerSearchPageResponse,
} from '../models/customers.response';
import { CustomersMockService } from './customers-mock.service';
import { CustomersService } from './customers.service';

describe('CustomersService', () => {
  const searchResponse: ObservedCustomerSearchPageResponse = {
    items: [],
    size: 0,
    hasMore: false,
    snapshotAt: '2026-08-08T14:00:00Z',
  };

  const detailResponse: ObservedCustomerDetailResponse = {
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
  };

  const paymentPageResponse: ObservedCustomerPaymentPageResponse = {
    items: [],
    size: 0,
    hasMore: false,
    snapshotAt: '2026-08-08T14:00:00Z',
  };

  function configure(usesApi: boolean) {
    const api = {
      search: vi.fn(() => of(searchResponse)),
      get: vi.fn(() => of(detailResponse)),
      payments: vi.fn(() => of(paymentPageResponse)),
    };

    const mock = {
      search: vi.fn(() => of(searchResponse)),
      get: vi.fn(() => of(detailResponse)),
      payments: vi.fn(() => of(paymentPageResponse)),
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        CustomersService,
        {
          provide: BackendModeService,
          useValue: { usesApi, usesMock: !usesApi },
        },
        { provide: CustomersApiClient, useValue: api },
        { provide: CustomersMockService, useValue: mock },
      ],
    });

    return {
      service: TestBed.inject(CustomersService),
      api,
      mock,
    };
  }

  it('uses the mock datasource in mock mode', async () => {
    const { service, api, mock } = configure(false);

    await firstValueFrom(service.search({ size: 10 }));

    expect(mock.search).toHaveBeenCalledOnce();
    expect(api.search).not.toHaveBeenCalled();
  });

  it('uses the API datasource in api mode', async () => {
    const { service, api, mock } = configure(true);

    await firstValueFrom(service.search({ size: 10 }));

    expect(api.search).toHaveBeenCalledOnce();
    expect(mock.search).not.toHaveBeenCalled();
  });

  it('uses the API datasource for linked Payments in api mode', async () => {
    const { service, api, mock } = configure(true);

    await firstValueFrom(service.payments('7cb96138-c2b7-4f61-8bb3-b3b00599f101', { size: 10 }));

    expect(api.payments).toHaveBeenCalledOnce();
    expect(mock.payments).not.toHaveBeenCalled();
  });

  it('maps API detail responses to application models', async () => {
    const { service } = configure(true);

    const detail = await firstValueFrom(service.get('7cb96138-c2b7-4f61-8bb3-b3b00599f101'));

    expect(detail?.firstObservedAt).toBeInstanceOf(Date);
    expect(detail?.legalName).toBe('CAMEROUN SERVICES SARL');
  });

  it('maps an API 404 detail to null for the existing not-found state', async () => {
    const { service, api } = configure(true);

    api.get.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
            statusText: 'Not Found',
          }),
      ),
    );

    const detail = await firstValueFrom(service.get('missing-customer'));

    expect(detail).toBeNull();
  });
});
