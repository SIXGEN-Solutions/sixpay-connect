import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import {
  AdministrationOverview,
  GeneralSettings,
  IntegrationStatus,
} from '../models/administration';

const OBSERVED_AT = new Date('2026-08-08T14:44:00Z');

const SETTINGS: GeneralSettings = {
  accountingCutoffZone: 'Africa/Douala',
  accountingCutoffTime: '23:59',
};

const INTEGRATIONS: readonly IntegrationStatus[] = [
  {
    integrationId: 'tresor-pay',
    name: 'TresorPay',
    type: 'REST / mTLS',
    health: 'AVAILABLE',
    detail: 'État de démonstration TresorPay.',
    lastSuccessfulAt: new Date('2026-08-08T14:43:42Z'),
    lastCheckedAt: new Date('2026-08-08T14:44:00Z'),
  },
  {
    integrationId: 'amplitude',
    name: 'Amplitude',
    type: 'Core Banking',
    health: 'AVAILABLE',
    detail: 'État simulé du Core Banking pour le mode mock.',
    lastSuccessfulAt: new Date('2026-08-08T14:42:31Z'),
    lastCheckedAt: new Date('2026-08-08T14:44:00Z'),
  },
];

@Injectable({ providedIn: 'root' })
export class AdministrationMockService {
  overview(): Observable<AdministrationOverview> {
    return of({
      settings: SETTINGS,
      integrations: INTEGRATIONS,
      observedAt: OBSERVED_AT,
    });
  }

  settings(): Observable<GeneralSettings> {
    return of(SETTINGS);
  }

  integrations(): Observable<readonly IntegrationStatus[]> {
    return of(INTEGRATIONS);
  }
}
