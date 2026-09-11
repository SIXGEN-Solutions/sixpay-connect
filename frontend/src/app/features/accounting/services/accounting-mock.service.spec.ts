import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { AccountingMockService } from './accounting-mock.service';

describe('AccountingMockService', () => {
  let service: AccountingMockService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AccountingMockService);
  });

  it('filters by contract-backed status', async () => {
    const batches = await firstValueFrom(
      service.search({
        status: 'NOT_COMPLETED',
      }),
    );

    expect(batches).toHaveLength(1);
    expect(batches[0]?.status).toBe('NOT_COMPLETED');
  });

  it('filters by business date', async () => {
    const batches = await firstValueFrom(
      service.search({
        businessDate: '2026-08-08',
      }),
    );

    expect(batches).toHaveLength(2);
    expect(batches.every((batch) => batch.businessDate === '2026-08-08')).toBe(true);
  });

  it('returns a batch detail', async () => {
    const batch = await firstValueFrom(service.get('11111111-1111-4111-8111-111111111111'));

    expect(batch?.items).toHaveLength(1);

    expect(batch?.status).toBe('NOT_COMPLETED');
  });

  it('returns null for an unknown batch', async () => {
    const batch = await firstValueFrom(service.get('ACC-UNKNOWN'));

    expect(batch).toBeNull();
  });
});
