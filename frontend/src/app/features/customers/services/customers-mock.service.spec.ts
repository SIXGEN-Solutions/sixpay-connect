import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { CustomersMockService } from './customers-mock.service';

describe('CustomersMockService', () => {
  let service: CustomersMockService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CustomersMockService);
  });

  it('supports cursor pagination', async () => {
    const first = await firstValueFrom(service.search({ size: 1 }));

    expect(first.items).toHaveLength(1);
    expect(first.hasMore).toBe(true);
    expect(first.nextCursor).toBe('1');

    const second = await firstValueFrom(
      service.search({ size: 1, cursor: first.nextCursor! }),
    );

    expect(second.items).toHaveLength(1);
    expect(second.hasMore).toBe(false);
  });

  it('filters by last Payment status', async () => {
    const page = await firstValueFrom(
      service.search({ lastPaymentStatus: 'POSTING', size: 50 }),
    );

    expect(page.items).toHaveLength(1);
    expect(page.items[0]?.legalName).toBe('ETS MBARGA & FILS');
  });

  it('returns null for an unknown ObservedCustomer', async () => {
    const customer = await firstValueFrom(
      service.get('00000000-0000-4000-8000-000000000000'),
    );

    expect(customer).toBeNull();
  });

  it('paginates linked Payment references', async () => {
    const first = await firstValueFrom(
      service.payments('7cb96138-c2b7-4f61-8bb3-b3b00599f101', { size: 2 }),
    );

    expect(first.items).toHaveLength(2);
    expect(first.hasMore).toBe(true);
    expect(first.nextCursor).toBe('2');
  });
});
