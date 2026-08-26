import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { ReportingApiClient } from '../api/reporting-api.client';
import {
  PaymentAuditExportJobResponse,
  PaymentAuditPageResponse,
  PaymentAuditRecordResponse,
  PaymentTimelinePageResponse,
} from '../models/reporting.response';
import { ReportingMockService } from './reporting-mock.service';
import { ReportingService } from './reporting.service';

describe('ReportingService', () => {
  const timelineResponse: PaymentTimelinePageResponse = {
    items: [],
    size: 0,
    hasMore: false,
    snapshotAt: '2026-08-08T14:00:00Z',
  };

  const auditPageResponse: PaymentAuditPageResponse = {
    items: [],
    size: 0,
    hasMore: false,
    snapshotAt: '2026-08-08T14:00:00Z',
  };

  const auditRecordResponse: PaymentAuditRecordResponse = {
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
  };

  const exportResponse: PaymentAuditExportJobResponse = {
    exportId: '33333333-3333-4333-8333-333333333301',
    status: 'AVAILABLE',
    requestedAt: '2026-08-08T14:05:00Z',
    requestedBy: 'local-auditor',
    businessPurpose: 'Internal audit validation',
    expiresAt: '2026-08-08T15:05:00Z',
  };

  function configure(usesApi: boolean) {
    const api = {
      timeline: vi.fn(() => of(timelineResponse)),
      searchAudit: vi.fn(() => of(auditPageResponse)),
      getAudit: vi.fn(() => of(auditRecordResponse)),
      requestExport: vi.fn(() => of(exportResponse)),
      getExport: vi.fn(() => of(exportResponse)),
    };

    const mock = {
      timeline: vi.fn(() => of(timelineResponse)),
      searchAudit: vi.fn(() => of(auditPageResponse)),
      getAudit: vi.fn(() => of(auditRecordResponse)),
      requestExport: vi.fn(() => of(exportResponse)),
      getExport: vi.fn(() => of(exportResponse)),
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        ReportingService,
        {
          provide: BackendModeService,
          useValue: {
            usesApi,
            usesMock: !usesApi,
          },
        },
        {
          provide: ReportingApiClient,
          useValue: api,
        },
        {
          provide: ReportingMockService,
          useValue: mock,
        },
      ],
    });

    return {
      service: TestBed.inject(ReportingService),
      api,
      mock,
    };
  }

  it('uses mock datasource for reporting in mock mode', async () => {
    const { service, api, mock } = configure(false);

    await firstValueFrom(
      service.timeline(
        '7fa85f64-5717-4562-b3fc-2c963f66afa1',
        { size: 10 },
      ),
    );

    expect(mock.timeline).toHaveBeenCalledOnce();
    expect(api.timeline).not.toHaveBeenCalled();
  });

  it('uses API datasource for audit search in api mode', async () => {
    const { service, api, mock } = configure(true);

    await firstValueFrom(
      service.searchAudit({
        occurredFrom: new Date('2026-08-08T00:00:00Z'),
        occurredTo: new Date('2026-08-09T00:00:00Z'),
        size: 10,
      }),
    );

    expect(api.searchAudit).toHaveBeenCalledOnce();
    expect(mock.searchAudit).not.toHaveBeenCalled();
  });

  it('uses API datasource for controlled export in api mode', async () => {
    const { service, api, mock } = configure(true);

    await firstValueFrom(
      service.requestExport({
        occurredFrom: '2026-08-08T00:00:00Z',
        occurredTo: '2026-08-09T00:00:00Z',
        businessPurpose: 'Internal audit validation',
        format: 'CSV',
      }),
    );

    expect(api.requestExport).toHaveBeenCalledOnce();
    expect(mock.requestExport).not.toHaveBeenCalled();
  });

  it('maps API audit details to application models', async () => {
    const { service } = configure(true);

    const record = await firstValueFrom(
      service.getAudit('22222222-2222-4222-8222-222222222201'),
    );

    expect(record?.occurredAt).toBeInstanceOf(Date);
    expect(record?.integrityScheme).toBe('HASH_CHAIN');
  });

  it('maps an audit detail API 404 to null', async () => {
    const { service, api } = configure(true);

    api.getAudit.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
            statusText: 'Not Found',
          }),
      ),
    );

    const record = await firstValueFrom(
      service.getAudit('missing-audit'),
    );

    expect(record).toBeNull();
  });

  it('maps an export API 404 to null', async () => {
    const { service, api } = configure(true);

    api.getExport.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
            statusText: 'Not Found',
          }),
      ),
    );

    const exportJob = await firstValueFrom(
      service.getExport('missing-export'),
    );

    expect(exportJob).toBeNull();
  });
});
