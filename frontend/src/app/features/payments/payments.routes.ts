import { Routes } from '@angular/router';

import { authorizationGuard } from '../../core/auth/authorization.guard';

const PAYMENT_READ_ROLES = ['ADMIN', 'MANAGER', 'AUDITOR'] as const;
const PAYMENT_READ_PERMISSIONS = ['payment.read'] as const;

export const PAYMENT_ROUTES: Routes = [
  {
    path: '',
    canActivate: [authorizationGuard],
    data: {
      roles: PAYMENT_READ_ROLES,
      permissions: PAYMENT_READ_PERMISSIONS,
      standaloneRoles: PAYMENT_READ_ROLES,
    },
    loadComponent: () =>
      import('./components/payment-list-page.component').then(
        (component) => component.PaymentListPageComponent,
      ),
  },
  {
    path: ':paymentId',
    canActivate: [authorizationGuard],
    data: {
      roles: PAYMENT_READ_ROLES,
      permissions: PAYMENT_READ_PERMISSIONS,
      standaloneRoles: PAYMENT_READ_ROLES,
    },
    loadComponent: () =>
      import('./components/payment-detail-page.component').then(
        (component) => component.PaymentDetailPageComponent,
      ),
  },
];
