import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { AccountingApiClient } from '../api/accounting-api.client';
import { AccountingMockService } from './accounting-mock.service';
import { AccountingService } from './accounting.service';

describe('AccountingService', () => {
  const api = {
    search: vi.fn(),
    get: vi.fn(),
  };

  const mock = {
    search: vi.fn(),
    get: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();

    TestBed.configureTestingModule({
      providers: [
        AccountingService,
        { provide: AccountingApiClient, useValue: api },
        { provide: AccountingMockService, useValue: mock },
        { provide: BackendModeService, useValue: { usesApi: true } },
      ],
    });
  });

  it('maps API search results', async () => {
    api.search.mockReturnValue(
      of({
        content: [
          {
            batchId: '11111111-1111-4111-8111-111111111111',
            businessDate: '2026-08-08',
            financialInstitutionCode: 'LAREGIONALE',
            status: 'COMPLETED',
            itemCount: 1,
            createdAt: '2026-08-08T18:01:00Z',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }),
    );

    const service = TestBed.inject(AccountingService);
    const result = await firstValueFrom(service.search({ status: 'COMPLETED' }));

    expect(result).toHaveLength(1);
    expect(result[0]?.status).toBe('COMPLETED');
    expect(result[0]?.createdAt).toBeInstanceOf(Date);
  });

  it('maps detail and items from the Query API response', async () => {
    api.get.mockReturnValue(
      of({
        batchId: '11111111-1111-4111-8111-111111111111',
        businessDate: '2026-08-08',
        financialInstitutionCode: 'LAREGIONALE',
        status: 'NOT_COMPLETED',
        itemCount: 1,
        createdAt: '2026-08-08T18:01:00Z',
        idempotencyKey: 'idem-1',
        items: [
          {
            paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afb3',
            publicPaymentReference: 'PAY-2026-0001801',
            partnerId: 'TRESORPAY',
            amount: 75000,
            currency: 'XAF',
            paymentOccurredAt: '2026-08-08T17:59:00Z',
            paymentBusinessDate: '2026-08-08',
            bankPostingReference: 'BANK-POST-001',
            tresorPayStatus: 'SUCCESS',
            tresorPayStatusCheckedAt: '2026-08-08T18:00:00Z',
            status: 'PENDING',
          },
        ],
      }),
    );

    const service = TestBed.inject(AccountingService);
    const result = await firstValueFrom(
      service.get('11111111-1111-4111-8111-111111111111'),
    );

    expect(result?.items).toHaveLength(1);
    expect(result?.items[0]?.bankPostingReference).toBe('BANK-POST-001');
    expect(result?.items[0]?.tresorPayStatus).toBe('SUCCESS');
    expect(result?.items[0]?.status).toBe('PENDING');
  });

  it('returns null only for HTTP 404 detail errors', async () => {
    api.get.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
            statusText: 'Not Found',
          }),
      ),
    );

    const service = TestBed.inject(AccountingService);
    const result = await firstValueFrom(service.get('missing'));

    expect(result).toBeNull();
  });

  it('propagates non-404 detail errors', async () => {
    api.get.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 500,
            statusText: 'Internal Server Error',
          }),
      ),
    );

    const service = TestBed.inject(AccountingService);

    await expect(firstValueFrom(service.get('broken'))).rejects.toBeInstanceOf(
      HttpErrorResponse,
    );
  });
});
