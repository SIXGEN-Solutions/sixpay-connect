import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { PaymentsMockService } from './payments-mock.service';

describe('PaymentsMockService', () => {
  let service: PaymentsMockService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PaymentsMockService);
  });

  it('filters by contract status', async () => {
    const page = await firstValueFrom(service.search({ status: 'REJECTED', size: 50 }));

    expect(page.items).toHaveLength(1);
    expect(page.items[0]?.status).toBe('REJECTED');
  });

  it('supports stable cursor pagination', async () => {
    const first = await firstValueFrom(service.search({ size: 2 }));

    expect(first.nextCursor).toBe('2');

    const second = await firstValueFrom(
      service.search({
        size: 2,
        cursor: first.nextCursor!,
      }),
    );

    expect(first.items).toHaveLength(2);
    expect(first.hasMore).toBe(true);
    expect(first.nextCursor).toBe('2');

    expect(second.items).toHaveLength(1);
    expect(second.hasMore).toBe(false);
  });

  it('returns null for an unknown payment id', async () => {
    const payment = await firstValueFrom(service.get('00000000-0000-4000-8000-000000000000'));

    expect(payment).toBeNull();
  });
});
