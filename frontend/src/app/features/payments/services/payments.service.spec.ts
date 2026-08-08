import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { PaymentsService } from './payments.service';

describe('PaymentsService', () => {
  let service: PaymentsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PaymentsService);
  });

  it('exposes application models instead of transport DTOs', async () => {
    const page = await firstValueFrom(service.search({ size: 2 }));

    expect(page.items[0]?.createdAt).toBeInstanceOf(Date);
    expect(page.snapshotAt).toBeInstanceOf(Date);
  });
});
