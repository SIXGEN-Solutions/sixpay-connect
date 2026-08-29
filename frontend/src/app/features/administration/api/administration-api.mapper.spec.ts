import {
  mapAdministrationOverviewResponse,
  mapAdministrationSettingsResponse,
  mapIntegrationStatusResponse,
} from './administration-api.mapper';

describe('administration API mapper', () => {
  it('maps settings without mock-only fields', () => {
    expect(
      mapAdministrationSettingsResponse({
        accountingCutoffZone: 'Africa/Douala',
        accountingCutoffTime: '23:59',
      }),
    ).toEqual({
      accountingCutoffZone: 'Africa/Douala',
      accountingCutoffTime: '23:59',
    });
  });

  it('maps integration dates', () => {
    const mapped = mapIntegrationStatusResponse({
      integrationId: 'amplitude',
      name: 'Amplitude',
      type: 'Core Banking',
      health: 'AVAILABLE',
      detail: 'Nominal',
      lastSuccessfulAt: '2026-08-23T10:00:00Z',
      lastCheckedAt: '2026-08-23T10:01:00Z',
    });

    expect(mapped.lastSuccessfulAt).toBeInstanceOf(Date);

    expect(mapped.lastCheckedAt).toBeInstanceOf(Date);
  });

  it('maps overview observedAt', () => {
    const mapped = mapAdministrationOverviewResponse({
      settings: {
        accountingCutoffZone: 'Africa/Douala',
        accountingCutoffTime: '23:59',
      },
      integrations: [],
      observedAt: '2026-08-23T10:02:00Z',
    });

    expect(mapped.observedAt).toBeInstanceOf(Date);
  });
});
