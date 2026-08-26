import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { PaymentsApiClient } from '../api/payments-api.client';
import { PaymentDetailResponse, PaymentSearchPageResponse } from '../models/payments.response';
import { PaymentsMockService } from './payments-mock.service';
import { PaymentsService } from './payments.service';

describe('PaymentsService', () => {
  const pageResponse: PaymentSearchPageResponse = {
    items: [],
    size: 0,
    hasMore: false,
    snapshotAt: '2026-08-08T14:00:00Z',
  };

  const detailResponse: PaymentDetailResponse = {
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
  };

  function configure(usesApi: boolean): {
    service: PaymentsService;
    api: { searchPayments: ReturnType<typeof vi.fn>; getPayment: ReturnType<typeof vi.fn> };
    mock: { search: ReturnType<typeof vi.fn>; get: ReturnType<typeof vi.fn> };
  } {
    const api = {
      searchPayments: vi.fn(() => of(pageResponse)),
      getPayment: vi.fn(() => of(detailResponse)),
    };
    const mock = {
      search: vi.fn(() => of(pageResponse)),
      get: vi.fn(() => of(detailResponse)),
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        PaymentsService,
        {
          provide: BackendModeService,
          useValue: { usesApi, usesMock: !usesApi },
        },
        { provide: PaymentsApiClient, useValue: api },
        { provide: PaymentsMockService, useValue: mock },
      ],
    });

    return { service: TestBed.inject(PaymentsService), api, mock };
  }

  it('uses the mock datasource in mock mode', async () => {
    const { service, api, mock } = configure(false);

    await firstValueFrom(service.search({ size: 10 }));

    expect(mock.search).toHaveBeenCalledOnce();
    expect(api.searchPayments).not.toHaveBeenCalled();
  });

  it('uses the real API datasource in api mode', async () => {
    const { service, api, mock } = configure(true);

    await firstValueFrom(service.search({ size: 10 }));

    expect(api.searchPayments).toHaveBeenCalledOnce();
    expect(mock.search).not.toHaveBeenCalled();
  });

  it('maps API detail responses to application models', async () => {
    const { service } = configure(true);

    const detail = await firstValueFrom(
      service.get('7fa85f64-5717-4562-b3fc-2c963f66afa1'),
    );

    expect(detail?.createdAt).toBeInstanceOf(Date);
    expect(detail?.paymentReference).toBe('PAY-2026-0001842');
  });

  it('maps an API 404 detail to null for the existing not-found UI state', async () => {
    const { service, api } = configure(true);

    api.getPayment.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
            statusText: 'Not Found',
          }),
      ),
    );

    const detail = await firstValueFrom(service.get('missing-payment'));

    expect(detail).toBeNull();
  });
});
