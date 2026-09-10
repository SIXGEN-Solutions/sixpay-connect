import { Routes } from '@angular/router';

import { authorizationGuard } from '../../core/auth/authorization.guard';

const CUSTOMER_READ_ROLES = ['ADMIN', 'MANAGER', 'AUDITOR'] as const;

export const CUSTOMER_ROUTES: Routes = [
  {
    path: '',
    canActivate: [authorizationGuard],
    data: {
      roles: CUSTOMER_READ_ROLES,
      permissions: ['customer.read'],
      standaloneRoles: CUSTOMER_READ_ROLES,
    },
    loadComponent: () =>
      import('./components/customer-master-list-page.component').then(
        (component) => component.CustomerMasterListPageComponent,
      ),
  },
  {
    path: 'enroll',
    canActivate: [authorizationGuard],
    data: {
      roles: ['ADMIN'],
      permissions: ['customer.create'],
      standaloneRoles: ['ADMIN'],
    },
    loadComponent: () =>
      import('./components/customer-enrollment-wizard.component').then(
        (component) => component.CustomerEnrollmentWizardComponent,
      ),
  },
  {
    path: 'observed',
    canActivate: [authorizationGuard],
    data: {
      roles: CUSTOMER_READ_ROLES,
      permissions: ['observed-customer.read'],
      standaloneRoles: CUSTOMER_READ_ROLES,
    },
    loadComponent: () =>
      import('./components/customer-list-page.component').then(
        (component) => component.CustomerListPageComponent,
      ),
  },
  {
    path: 'observed/:observedCustomerId',
    canActivate: [authorizationGuard],
    data: {
      roles: CUSTOMER_READ_ROLES,
      permissions: ['observed-customer.read'],
      standaloneRoles: CUSTOMER_READ_ROLES,
    },
    loadComponent: () =>
      import('./components/customer-detail-page.component').then(
        (component) => component.CustomerDetailPageComponent,
      ),
  },
  {
    path: ':customerId',
    canActivate: [authorizationGuard],
    data: {
      roles: CUSTOMER_READ_ROLES,
      permissions: ['customer.read', 'subscription.read'],
      standaloneRoles: CUSTOMER_READ_ROLES,
    },
    loadComponent: () =>
      import('./components/customer-master-detail-page.component').then(
        (component) => component.CustomerMasterDetailPageComponent,
      ),
  },
];
