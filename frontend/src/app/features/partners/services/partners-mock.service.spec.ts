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

  it('returns three distinct pages for size ten', async () => {
    const first = await firstValueFrom(
      service.search({ page: 0, size: 10 }),
    );
    const second = await firstValueFrom(
      service.search({ page: 1, size: 10 }),
    );
    const third = await firstValueFrom(
      service.search({ page: 2, size: 10 }),
    );

    expect(first.items).toHaveLength(10);
    expect(second.items).toHaveLength(10);
    expect(third.items).toHaveLength(8);

    expect(first.totalElements).toBe(28);
    expect(first.totalPages).toBe(3);

    const ids = [
      ...first.items,
      ...second.items,
      ...third.items,
    ].map((item) => item.id);

    expect(new Set(ids).size).toBe(28);
  });

  it('returns an empty page beyond the last page without corrupting totals', async () => {
    const page = await firstValueFrom(
      service.search({ page: 3, size: 10 }),
    );

    expect(page.items).toHaveLength(0);
    expect(page.page).toBe(3);
    expect(page.size).toBe(10);
    expect(page.totalElements).toBe(28);
    expect(page.totalPages).toBe(3);
  });

  it('returns an empty catalog for the empty scenario', async () => {
    scenario.setScenario('empty');

    const page = await firstValueFrom(
      service.search({ page: 0, size: 20 }),
    );

    expect(page.items).toHaveLength(0);
    expect(page.totalElements).toBe(0);
    expect(page.totalPages).toBe(0);
  });

  it('keeps list detail and status coherent', async () => {
    const page = await firstValueFrom(
      service.search({ page: 0, size: 20 }),
    );
    const selected = page.items[0]!;

    const detail = await firstValueFrom(
      service.get(selected.id),
    );
    const status = await firstValueFrom(
      service.getStatus(selected.id),
    );

    expect(detail.id).toBe(selected.id);
    expect(detail.legalName).toBe(selected.legalName);
    expect(detail.status).toBe(selected.status);
    expect(status.partnerId).toBe(selected.id);
    expect(status.status).toBe(selected.status);
  });

  it('keeps lifecycle changes visible through subsequent reads', async () => {
    const id = '10000000-0000-4000-8000-000000000004';

    await firstValueFrom(
      service.decide(id, { decision: 'APPROVE' }),
    );
    await firstValueFrom(
      service.suspend(id, { reason: 'Compliance hardening test' }),
    );

    const suspended = await firstValueFrom(service.get(id));
    expect(suspended.status).toBe('SUSPENDED');

    await firstValueFrom(service.reactivate(id));

    const active = await firstValueFrom(service.get(id));
    const status = await firstValueFrom(service.getStatus(id));

    expect(active.status).toBe('ACTIVE');
    expect(status.status).toBe('ACTIVE');
    expect(status.connection.newTransactionsAllowed).toBe(true);
  });
});
