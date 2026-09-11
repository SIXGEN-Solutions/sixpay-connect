import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { ReportingMockService } from './reporting-mock.service';

describe('ReportingMockService', () => {
  let service: ReportingMockService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ReportingMockService);
  });

  it('returns a timeline for the known payment', async () => {
    const page = await firstValueFrom(
      service.timeline('7fa85f64-5717-4562-b3fc-2c963f66afa1', { size: 50 }),
    );

    expect(page.items.length).toBeGreaterThan(0);
  });

  it('filters audit evidence within a required period', async () => {
    const page = await firstValueFrom(
      service.searchAudit({
        occurredFrom: new Date('2026-08-08T00:00:00Z'),
        occurredTo: new Date('2026-08-09T00:00:00Z'),
        result: 'SUCCESS',
        size: 50,
      }),
    );

    expect(page.items).toHaveLength(3);
  });

  it('paginates the timeline with a stable cursor', async () => {
    const first = await firstValueFrom(
      service.timeline('7fa85f64-5717-4562-b3fc-2c963f66afa1', { size: 2 }),
    );

    expect(first.items).toHaveLength(2);
    expect(first.hasMore).toBe(true);
    expect(first.nextCursor).toBe('2');

    const timelineCursor = first.nextCursor;
    expect(timelineCursor).toBeTruthy();

    const second = await firstValueFrom(
      service.timeline('7fa85f64-5717-4562-b3fc-2c963f66afa1', {
        size: 2,
        ...(timelineCursor ? { cursor: timelineCursor } : {}),
      }),
    );

    expect(second.items).toHaveLength(2);
    expect(second.items[0]?.timelineEntryId).not.toBe(first.items[0]?.timelineEntryId);
  });

  it('paginates audit evidence with a stable cursor', async () => {
    const query = {
      occurredFrom: new Date('2026-08-08T00:00:00Z'),
      occurredTo: new Date('2026-08-09T00:00:00Z'),
      size: 2,
    } as const;

    const first = await firstValueFrom(service.searchAudit(query));

    expect(first.items).toHaveLength(2);
    expect(first.hasMore).toBe(true);
    expect(first.nextCursor).toBe('2');

    const auditCursor = first.nextCursor;
    expect(auditCursor).toBeTruthy();

    const second = await firstValueFrom(
      service.searchAudit({
        ...query,
        ...(auditCursor ? { cursor: auditCursor } : {}),
      }),
    );

    expect(second.items).toHaveLength(1);
    expect(second.hasMore).toBe(false);
  });

  it('creates a controlled export job', async () => {
    const job = await firstValueFrom(
      service.requestExport({
        occurredFrom: '2026-08-08T00:00:00Z',
        occurredTo: '2026-08-09T00:00:00Z',
        businessPurpose: 'Internal audit validation purpose',
        format: 'CSV',
      }),
    );

    expect(job.status).toBe('AVAILABLE');
    expect(job.recordCount).toBe(3);
  });
});
