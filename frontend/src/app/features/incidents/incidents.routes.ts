import { Routes } from '@angular/router';

import { incidentsGuard } from './guards/incidents.guard';

export const INCIDENT_ROUTES: Routes = [
  {
    path: '',
    canActivate: [incidentsGuard],
    loadComponent: () =>
      import('./components/incident-list-page.component').then(
        (component) => component.IncidentListPageComponent,
      ),
  },
  {
    path: ':incidentId',
    canActivate: [incidentsGuard],
    loadComponent: () =>
      import('./components/incident-detail-page.component').then(
        (component) => component.IncidentDetailPageComponent,
      ),
  },
];
