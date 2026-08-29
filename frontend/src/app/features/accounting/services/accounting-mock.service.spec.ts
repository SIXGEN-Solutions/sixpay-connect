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
