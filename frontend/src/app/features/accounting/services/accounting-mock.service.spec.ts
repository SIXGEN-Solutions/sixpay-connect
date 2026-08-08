import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { AccountingMockService } from './accounting-mock.service';

describe('AccountingMockService', () => {
  let service: AccountingMockService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AccountingMockService);
  });

  it('filters by reconciliation status', async () => {
    const batches = await firstValueFrom(
      service.search({ reconciliationStatus: 'PARTIAL_MATCH' }),
    );

    expect(batches).toHaveLength(1);
    expect(batches[0]?.batchId).toBe('ACC-20260808-03');
  });

  it('returns a batch detail', async () => {
    const batch = await firstValueFrom(service.get('ACC-20260808-03'));

    expect(batch?.discrepancies).toHaveLength(2);
    expect(batch?.status).toBe('RECONCILING');
  });

  it('returns null for an unknown batch', async () => {
    const batch = await firstValueFrom(service.get('ACC-UNKNOWN'));

    expect(batch).toBeNull();
  });
});
