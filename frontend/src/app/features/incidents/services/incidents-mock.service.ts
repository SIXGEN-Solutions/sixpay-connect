import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { IncidentQuery } from '../models/incident-query';
import { IncidentDetail } from '../models/incidents';

const INCIDENTS: readonly IncidentDetail[] = [
  {
    incidentId: 'INC-2026-0084',
    severity: 'HIGH',
    component: 'Accounting',
    summary: 'Écarts de réconciliation',
    status: 'INVESTIGATING',
    openedAt: new Date('2026-08-08T17:46:00Z'),
    updatedAt: new Date('2026-08-08T18:07:00Z'),
    description: 'Huit paiements ne sont pas parfaitement rapprochés avec le résultat provider.',
    impact: 'Pas de perte financière confirmée. Le lot reste en contrôle avant clôture.',
    accountingBatchId: 'ACC-20260808-03',
    paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afb3',
    paymentReference: 'PAY-2026-0001801',
    correlationId: 'f77793b0-7d50-4d9f-8f4f-e67903f35284',
    timeline: [
      {
        eventId: 'INC-EVT-084-1',
        occurredAt: new Date('2026-08-08T17:46:00Z'),
        message: 'Alerte de rapprochement déclenchée.',
        actor: 'reconciliation-monitor',
      },
      {
        eventId: 'INC-EVT-084-2',
        occurredAt: new Date('2026-08-08T17:49:00Z'),
        message: 'Incident créé automatiquement.',
        actor: 'incident-service',
      },
      {
        eventId: 'INC-EVT-084-3',
        occurredAt: new Date('2026-08-08T17:55:00Z'),
        message: 'Analyse opérateur démarrée.',
        actor: 'ops-user',
      },
      {
        eventId: 'INC-EVT-084-4',
        occurredAt: new Date('2026-08-08T18:07:00Z'),
        message: 'Six écarts sur huit catégorisés.',
        actor: 'ops-user',
      },
    ],
  },
  {
    incidentId: 'INC-2026-0083',
    severity: 'MEDIUM',
    component: 'Notification',
    summary: 'Latence TresorPay',
    status: 'MONITORING',
    openedAt: new Date('2026-08-08T17:28:00Z'),
    updatedAt: new Date('2026-08-08T18:01:00Z'),
    description: 'Hausse temporaire de la latence de livraison des notifications vers TresorPay.',
    impact: 'Paiements non affectés. Retour utilisateur potentiellement retardé.',
    accountingBatchId: null,
    paymentId: null,
    paymentReference: null,
    correlationId: '1aab977e-62ba-4432-a649-3709564173b4',
    timeline: [
      {
        eventId: 'INC-EVT-083-1',
        occurredAt: new Date('2026-08-08T17:28:00Z'),
        message: 'Seuil de latence dépassé.',
        actor: 'notification-monitor',
      },
      {
        eventId: 'INC-EVT-083-2',
        occurredAt: new Date('2026-08-08T18:01:00Z'),
        message: 'Latence revenue sous le seuil, surveillance maintenue.',
        actor: 'ops-user',
      },
    ],
  },
  {
    incidentId: 'INC-2026-0082',
    severity: 'LOW',
    component: 'Payment',
    summary: 'Timeout isolé de vérification',
    status: 'RESOLVED',
    openedAt: new Date('2026-08-08T15:10:00Z'),
    updatedAt: new Date('2026-08-08T15:22:00Z'),
    description: 'Un timeout isolé a été observé pendant une vérification bancaire.',
    impact: 'Une opération différée, sans double débit.',
    accountingBatchId: null,
    paymentId: '7fa85f64-5717-4562-b3fc-2c963f66afa2',
    paymentReference: 'PAY-2026-0001841',
    correlationId: 'a67cf248-11d2-49f1-a38b-cff0fe4d9547',
    timeline: [
      {
        eventId: 'INC-EVT-082-1',
        occurredAt: new Date('2026-08-08T15:10:00Z'),
        message: 'Timeout détecté.',
        actor: 'payment-monitor',
      },
      {
        eventId: 'INC-EVT-082-2',
        occurredAt: new Date('2026-08-08T15:22:00Z'),
        message: 'Résultat confirmé et incident résolu.',
        actor: 'ops-user',
      },
    ],
  },
];

@Injectable({ providedIn: 'root' })
export class IncidentsMockService {
  search(query: IncidentQuery): Observable<readonly IncidentDetail[]> {
    return of(
      INCIDENTS.filter(
        (incident) =>
          (!query.severity || incident.severity === query.severity) &&
          (!query.status || incident.status === query.status) &&
          (!query.component ||
            incident.component.toLowerCase().includes(query.component.toLowerCase())),
      ),
    );
  }

  get(incidentId: string): Observable<IncidentDetail | null> {
    return of(INCIDENTS.find((incident) => incident.incidentId === incidentId) ?? null);
  }
}
