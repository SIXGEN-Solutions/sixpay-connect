import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AccountingApiClient } from './accounting-api.client';

describe('AccountingApiClient', () => {
  let client: AccountingApiClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    client = TestBed.inject(AccountingApiClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('sends contract-backed filters and paging', () => {
    client
      .search({
        businessDate: '2026-08-08',
        status: 'NOT_COMPLETED',
        page: 2,
        size: 50,
      })
      .subscribe();

    const request = controller.expectOne(
      (candidate) => candidate.url === '/internal/api/v1/accounting-batches',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('businessDate')).toBe('2026-08-08');
    expect(request.request.params.get('status')).toBe('NOT_COMPLETED');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('50');

    request.flush({
      content: [],
      page: 2,
      size: 50,
      totalElements: 0,
      totalPages: 0,
    });
  });

  it('uses contract defaults when paging is omitted', () => {
    client.search({}).subscribe();

    const request = controller.expectOne(
      (candidate) => candidate.url === '/internal/api/v1/accounting-batches',
    );

    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');

    request.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
  });

  it('calls the accounting batch detail endpoint', () => {
    client.get('11111111-1111-4111-8111-111111111111').subscribe();

    const request = controller.expectOne(
      '/internal/api/v1/accounting-batches/11111111-1111-4111-8111-111111111111',
    );

    expect(request.request.method).toBe('GET');

    request.flush({
      batchId: '11111111-1111-4111-8111-111111111111',
      businessDate: '2026-08-08',
      financialInstitutionCode: 'LAREGIONALE',
      status: 'NOT_COMPLETED',
      itemCount: 1,
      createdAt: '2026-08-08T18:01:00Z',
      idempotencyKey: 'idem-1',
      items: [],
    });
  });
});
