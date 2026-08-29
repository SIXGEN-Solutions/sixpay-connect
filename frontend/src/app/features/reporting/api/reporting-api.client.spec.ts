import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ReportingApiClient } from './reporting-api.client';

describe('ReportingApiClient', () => {
  let client: ReportingApiClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    client = TestBed.inject(ReportingApiClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('calls the published Payment timeline endpoint', () => {
    client
      .timeline('7fa85f64-5717-4562-b3fc-2c963f66afa1', {
        category: 'ACCOUNTING',
        occurredFrom: new Date('2026-08-08T00:00:00Z'),
        occurredTo: new Date('2026-08-09T00:00:00Z'),
        cursor: 'timeline-cursor',
        size: 50,
      })
      .subscribe();

    const request = controller.expectOne(
      (candidate) =>
        candidate.url === '/internal/api/v1/payments/7fa85f64-5717-4562-b3fc-2c963f66afa1/timeline',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('category')).toBe('ACCOUNTING');
    expect(request.request.params.get('occurredFrom')).toBe('2026-08-08T00:00:00.000Z');
    expect(request.request.params.get('occurredTo')).toBe('2026-08-09T00:00:00.000Z');
    expect(request.request.params.get('cursor')).toBe('timeline-cursor');
    expect(request.request.params.get('size')).toBe('50');

    request.flush({
      items: [],
      size: 0,
      hasMore: false,
      snapshotAt: '2026-08-08T14:00:00Z',
    });
  });

  it('calls the published audit search endpoint with required period', () => {
    client
      .searchAudit({
        paymentReference: 'PAY-2026-0001842',
        actorType: 'SERVICE',
        result: 'SUCCESS',
        sourceSystem: 'SIXPAY',
        occurredFrom: new Date('2026-08-08T00:00:00Z'),
        occurredTo: new Date('2026-08-09T00:00:00Z'),
        sort: 'OCCURRED_AT_DESC',
        cursor: 'audit-cursor',
        size: 25,
      })
      .subscribe();

    const request = controller.expectOne(
      (candidate) => candidate.url === '/internal/api/v1/payment-audit-records',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('paymentReference')).toBe('PAY-2026-0001842');
    expect(request.request.params.get('actorType')).toBe('SERVICE');
    expect(request.request.params.get('result')).toBe('SUCCESS');
    expect(request.request.params.get('sourceSystem')).toBe('SIXPAY');
    expect(request.request.params.get('occurredFrom')).toBe('2026-08-08T00:00:00.000Z');
    expect(request.request.params.get('occurredTo')).toBe('2026-08-09T00:00:00.000Z');
    expect(request.request.params.get('sort')).toBe('OCCURRED_AT_DESC');
    expect(request.request.params.get('cursor')).toBe('audit-cursor');
    expect(request.request.params.get('size')).toBe('25');

    request.flush({
      items: [],
      size: 0,
      hasMore: false,
      snapshotAt: '2026-08-08T14:00:00Z',
    });
  });

  it('calls the published audit detail endpoint', () => {
    client.getAudit('22222222-2222-4222-8222-222222222201').subscribe();

    const request = controller.expectOne(
      '/internal/api/v1/payment-audit-records/22222222-2222-4222-8222-222222222201',
    );

    expect(request.request.method).toBe('GET');

    request.flush({
      auditId: '22222222-2222-4222-8222-222222222201',
      occurredAt: '2026-08-08T13:47:12Z',
      actor: {
        actorType: 'SYSTEM',
        actorId: 'sixpay',
      },
      action: 'PAYMENT_REQUESTED',
      targetType: 'PAYMENT',
      targetId: '7fa85f64-5717-4562-b3fc-2c963f66afa1',
      result: 'SUCCESS',
      reasonCode: 'REQUEST_ACCEPTED',
      correlationId: 'e5e41af6-6f71-4cf2-a111-42837d1ea100',
      sourceSystem: 'SIXPAY',
      integrityEvidence: {
        scheme: 'HASH_CHAIN',
        value: 'sha256:test',
      },
    });
  });

  it('requests a controlled audit export', () => {
    client
      .requestExport({
        occurredFrom: '2026-08-08T00:00:00Z',
        occurredTo: '2026-08-09T00:00:00Z',
        businessPurpose: 'Internal audit validation',
        format: 'CSV',
      })
      .subscribe();

    const request = controller.expectOne('/internal/api/v1/payment-audit-exports');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      occurredFrom: '2026-08-08T00:00:00Z',
      occurredTo: '2026-08-09T00:00:00Z',
      businessPurpose: 'Internal audit validation',
      format: 'CSV',
    });

    request.flush({
      exportId: '33333333-3333-4333-8333-333333333301',
      status: 'ACCEPTED',
      requestedAt: '2026-08-08T14:05:00Z',
      requestedBy: 'local-auditor',
      businessPurpose: 'Internal audit validation',
      expiresAt: '2026-08-08T15:05:00Z',
    });
  });

  it('calls the published audit export status endpoint', () => {
    client.getExport('33333333-3333-4333-8333-333333333301').subscribe();

    const request = controller.expectOne(
      '/internal/api/v1/payment-audit-exports/33333333-3333-4333-8333-333333333301',
    );

    expect(request.request.method).toBe('GET');

    request.flush({
      exportId: '33333333-3333-4333-8333-333333333301',
      status: 'AVAILABLE',
      requestedAt: '2026-08-08T14:05:00Z',
      requestedBy: 'local-auditor',
      businessPurpose: 'Internal audit validation',
      expiresAt: '2026-08-08T15:05:00Z',
    });
  });
});
