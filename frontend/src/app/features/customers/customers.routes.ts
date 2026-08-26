import { Routes } from '@angular/router';

import { roleGuard } from '../../core/auth/role.guard';

const CUSTOMER_READ_ROLES = ['ADMIN', 'MANAGER', 'AUDITOR'] as const;

export const CUSTOMER_ROUTES: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: CUSTOMER_READ_ROLES },
    loadComponent: () =>
      import('./components/customer-master-list-page.component').then(
        (component) => component.CustomerMasterListPageComponent,
      ),
  },
  {
    path: 'enroll',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./components/customer-enrollment-wizard.component').then(
        (component) => component.CustomerEnrollmentWizardComponent,
      ),
  },
  {
    path: 'observed',
    canActivate: [roleGuard],
    data: { roles: CUSTOMER_READ_ROLES },
    loadComponent: () =>
      import('./components/customer-list-page.component').then(
        (component) => component.CustomerListPageComponent,
      ),
  },
  {
    path: 'observed/:observedCustomerId',
    canActivate: [roleGuard],
    data: { roles: CUSTOMER_READ_ROLES },
    loadComponent: () =>
      import('./components/customer-detail-page.component').then(
        (component) => component.CustomerDetailPageComponent,
      ),
  },
  {
    path: ':customerId',
    canActivate: [roleGuard],
    data: { roles: CUSTOMER_READ_ROLES },
    loadComponent: () =>
      import('./components/customer-master-detail-page.component').then(
        (component) => component.CustomerMasterDetailPageComponent,
      ),
  },
];
