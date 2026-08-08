import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import {
  AdministrationOverview,
  GeneralSettings,
  IntegrationStatus,
} from '../models/administration';

const SETTINGS: GeneralSettings = {
  accountingBatchSize: 500,
  paymentTimeoutMs: 5000,
  operationalRetentionDays: 90,
  maxPaymentAmountXaf: 5000000,
  maintenanceMode: false,
  updatedAt: new Date('2026-08-08T14:20:00Z'),
  updatedBy: 'admin@sixpay.local',
};

const INTEGRATIONS: readonly IntegrationStatus[] = [
  {
    integrationId: 'tresor-pay',
    name: 'TresorPay',
    type: 'REST / mTLS',
    health: 'AVAILABLE',
    detail: 'Dernier échange de démonstration nominal.',
    lastSuccessfulAt: new Date('2026-08-08T14:43:42Z'),
    lastCheckedAt: new Date('2026-08-08T14:44:00Z'),
  },
  {
    integrationId: 'amplitude',
    name: 'Amplitude',
    type: 'Core Banking',
    health: 'AVAILABLE',
    detail: 'État simulé du Core Banking pour la maquette frontend.',
    lastSuccessfulAt: new Date('2026-08-08T14:42:31Z'),
    lastCheckedAt: new Date('2026-08-08T14:44:00Z'),
  },
  {
    integrationId: 'accounting',
    name: 'Accounting',
    type: 'Internal capability',
    health: 'DEGRADED',
    detail: 'Réconciliation partielle sur le lot ACC-20260808-03.',
    lastSuccessfulAt: new Date('2026-08-08T14:07:00Z'),
    lastCheckedAt: new Date('2026-08-08T14:44:00Z'),
  },
  {
    integrationId: 'notifications',
    name: 'Notifications',
    type: 'Operational',
    health: 'AVAILABLE',
    detail: 'File de démonstration nominale.',
    lastSuccessfulAt: new Date('2026-08-08T14:43:50Z'),
    lastCheckedAt: new Date('2026-08-08T14:44:00Z'),
  },
];

@Injectable({ providedIn: 'root' })
export class AdministrationMockService {
  overview(): Observable<AdministrationOverview> {
    return of({ settings: SETTINGS, integrations: INTEGRATIONS });
  }

  settings(): Observable<GeneralSettings> {
    return of(SETTINGS);
  }

  integrations(): Observable<readonly IntegrationStatus[]> {
    return of(INTEGRATIONS);
  }
}
