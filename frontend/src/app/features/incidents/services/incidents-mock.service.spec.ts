import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { IncidentsMockService } from './incidents-mock.service';

describe('IncidentsMockService', () => {
  let service: IncidentsMockService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(IncidentsMockService);
  });

  it('filters incidents by severity', async () => {
    const incidents = await firstValueFrom(service.search({ severity: 'HIGH' }));

    expect(incidents).toHaveLength(1);
    expect(incidents[0]?.incidentId).toBe('INC-2026-0084');
  });

  it('filters incidents by component', async () => {
    const incidents = await firstValueFrom(service.search({ component: 'Notification' }));

    expect(incidents).toHaveLength(1);
    expect(incidents[0]?.status).toBe('MONITORING');
  });

  it('returns null for an unknown incident', async () => {
    const incident = await firstValueFrom(service.get('INC-UNKNOWN'));

    expect(incident).toBeNull();
  });
});
