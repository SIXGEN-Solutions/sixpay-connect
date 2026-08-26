import { Routes } from '@angular/router';

import { partnerRoleGuard } from './guards/partners.guard';

export const PARTNER_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [partnerRoleGuard],
    data: { roles: ['ADMIN', 'MANAGER', 'AUDITOR'] },
    loadComponent: () =>
      import('./components/partner-access-page.component').then(
        (component) => component.PartnerAccessPageComponent,
      ),
  },
  {
    path: 'status',
    canActivate: [partnerRoleGuard],
    data: { roles: ['PARTNER'] },
    loadComponent: () =>
      import('./components/partner-status-page.component').then(
        (component) => component.PartnerStatusPageComponent,
      ),
  },
  {
    path: 'create',
    canActivate: [partnerRoleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./components/partner-create-page.component').then(
        (component) => component.PartnerCreatePageComponent,
      ),
  },
  {
    path: ':partnerId',
    canActivate: [partnerRoleGuard],
    data: { roles: ['ADMIN', 'MANAGER', 'AUDITOR'] },
    loadComponent: () =>
      import('./components/partner-detail-page.component').then(
        (component) => component.PartnerDetailPageComponent,
      ),
  },
];
