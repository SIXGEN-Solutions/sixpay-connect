import { Routes } from '@angular/router';

import { authenticationGuard } from '../../core/auth/authentication.guard';
import { partnerRoleGuard } from './guards/partners.guard';

export const PARTNER_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [authenticationGuard],
    loadComponent: () =>
      import('./components/partner-access-page.component').then(
        (component) => component.PartnerAccessPageComponent,
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
    canActivate: [authenticationGuard],
    loadComponent: () =>
      import('./components/partner-detail-page.component').then(
        (component) => component.PartnerDetailPageComponent,
      ),
  },
];
