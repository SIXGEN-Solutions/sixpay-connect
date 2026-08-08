import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { CustomersService } from './customers.service';

describe('CustomersService', () => {
  let service: CustomersService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CustomersService);
  });

  it('exposes application models instead of transport DTOs', async () => {
    const page = await firstValueFrom(service.search({ size: 1 }));

    expect(page.items[0]?.lastObservedAt).toBeInstanceOf(Date);
    expect(page.snapshotAt).toBeInstanceOf(Date);
  });
});
