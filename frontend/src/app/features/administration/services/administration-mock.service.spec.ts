import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { AdministrationMockService } from './administration-mock.service';

describe('AdministrationMockService', () => {
  let service: AdministrationMockService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AdministrationMockService);
  });

  it('returns the administration overview', async () => {
    const overview = await firstValueFrom(service.overview());

    expect(overview.integrations).toHaveLength(4);
    expect(overview.settings.accountingBatchSize).toBe(500);
  });

  it('exposes the degraded accounting integration', async () => {
    const integrations = await firstValueFrom(service.integrations());
    const accounting = integrations.find((integration) => integration.integrationId === 'accounting');

    expect(accounting?.health).toBe('DEGRADED');
  });

  it('keeps settings as read-only mock data', async () => {
    const settings = await firstValueFrom(service.settings());

    expect(settings.maintenanceMode).toBe(false);
    expect(settings.updatedAt).toBeInstanceOf(Date);
  });
});
