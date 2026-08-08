import { Routes } from '@angular/router';

import { roleGuard } from '../../core/auth/role.guard';

const CUSTOMER_READ_ROLES = ['ADMIN', 'MANAGER', 'AUDITOR'] as const;

export const CUSTOMER_ROUTES: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: CUSTOMER_READ_ROLES },
    loadComponent: () =>
      import('./components/customer-list-page.component').then((component) => component.CustomerListPageComponent),
  },
  {
    path: ':observedCustomerId',
    canActivate: [roleGuard],
    data: { roles: CUSTOMER_READ_ROLES },
    loadComponent: () =>
      import('./components/customer-detail-page.component').then((component) => component.CustomerDetailPageComponent),
  },
];
