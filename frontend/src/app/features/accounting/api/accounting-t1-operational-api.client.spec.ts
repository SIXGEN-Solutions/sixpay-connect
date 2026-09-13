import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AccountingApiClient } from './accounting-api.client';

describe('AccountingApiClient T1 operational query', () => {
  let client: AccountingApiClient;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AccountingApiClient, provideHttpClient(), provideHttpClientTesting()],
    });

    client = TestBed.inject(AccountingApiClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('searches T1 operations with approved filters', () => {
    client
      .searchT1Operations({
        businessDate: '2026-09-11',
        status: 'ELIGIBLE_FOR_BATCH',
        paymentReference: ' PAY-001 ',
        page: 0,
        size: 20,
      })
      .subscribe();

    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/internal/api/v1/accounting-t1-operations' &&
        candidate.params.get('businessDate') === '2026-09-11' &&
        candidate.params.get('status') === 'ELIGIBLE_FOR_BATCH' &&
        candidate.params.get('paymentReference') === 'PAY-001' &&
        candidate.params.get('page') === '0' &&
        candidate.params.get('size') === '20',
    );

    expect(request.request.method).toBe('GET');
    request.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
  });

  it('loads one T1 operational candidate', () => {
    client.getT1Operation('candidate/1').subscribe();

    const request = http.expectOne('/internal/api/v1/accounting-t1-operations/candidate%2F1');

    expect(request.request.method).toBe('GET');
    request.flush({
      candidateId: 'candidate/1',
      paymentId: 'payment-1',
      publicPaymentReference: 'PAY-001',
      financialInstitutionCode: 'LAREGIONALE',
      accountingBusinessDate: '2026-09-11',
      status: 'ELIGIBLE_FOR_BATCH',
      tresorPayProviderStatus: 'COMPLETED',
      tresorPayCheckedAt: '2026-09-11T10:01:00Z',
      eligibilityReason: 'NONE',
      selectionBusinessDate: '2026-09-11',
      selectionFromInclusive: '2026-09-11T00:00:00Z',
      selectionToExclusive: '2026-09-12T00:00:00Z',
      technicalIssue: 'NONE',
      batchId: null,
    });
  });
});
