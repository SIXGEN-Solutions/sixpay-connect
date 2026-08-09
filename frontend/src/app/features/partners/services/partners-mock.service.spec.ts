import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { MockScenarioService } from '../../../core/mock/mock-scenario.service';
import { PartnersMockService } from './partners-mock.service';

describe('PartnersMockService', () => {
  let service: PartnersMockService;
  let scenario: MockScenarioService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PartnersMockService,
        MockScenarioService,
      ],
    });

    service = TestBed.inject(PartnersMockService);
    scenario = TestBed.inject(MockScenarioService);
  });

  it('returns a stable first page and supports pagination', async () => {
    const first = await firstValueFrom(
      service.search({ page: 0, size: 10 }),
    );
    const second = await firstValueFrom(
      service.search({ page: 1, size: 10 }),
    );

    expect(first.items).toHaveLength(10);
    expect(second.items).toHaveLength(10);
    expect(first.totalElements).toBe(28);
    expect(first.totalPages).toBe(3);
    expect(first.items[0]!.id).not.toBe(second.items[0]!.id);
  });

  it('returns an empty page for the empty scenario', async () => {
    scenario.setScenario('empty');

    const page = await firstValueFrom(
      service.search({ page: 0, size: 20 }),
    );

    expect(page.items).toHaveLength(0);
    expect(page.totalElements).toBe(0);
  });

  it('keeps list and detail data coherent', async () => {
    const page = await firstValueFrom(
      service.search({ page: 0, size: 20 }),
    );
    const selected = page.items[0]!;

    const detail = await firstValueFrom(
      service.get(selected.id),
    );

    expect(detail.id).toBe(selected.id);
    expect(detail.legalName).toBe(selected.legalName);
    expect(detail.status).toBe(selected.status);
  });

  it('updates mock lifecycle state consistently', async () => {
    const id = '10000000-0000-4000-8000-000000000004';

    const approved = await firstValueFrom(
      service.decide(id, { decision: 'APPROVE' }),
    );
    const status = await firstValueFrom(
      service.getStatus(id),
    );

    expect(approved.status).toBe('ACTIVE');
    expect(status.status).toBe('ACTIVE');
    expect(status.connection.newTransactionsAllowed).toBe(true);
  });
});
