import { Routes } from '@angular/router';

import { roleGuard } from '../../core/auth/role.guard';

const INCIDENT_READ_ROLES = ['ADMIN', 'MANAGER', 'AUDITOR'] as const;

export const INCIDENT_ROUTES: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: INCIDENT_READ_ROLES },
    loadComponent: () =>
      import('./components/incident-list-page.component').then((component) => component.IncidentListPageComponent),
  },
  {
    path: ':incidentId',
    canActivate: [roleGuard],
    data: { roles: INCIDENT_READ_ROLES },
    loadComponent: () =>
      import('./components/incident-detail-page.component').then((component) => component.IncidentDetailPageComponent),
  },
];
