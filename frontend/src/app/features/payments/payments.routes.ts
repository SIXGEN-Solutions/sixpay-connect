import { Routes } from '@angular/router';

import { roleGuard } from '../../core/auth/role.guard';

const PAYMENT_READ_ROLES = ['ADMIN', 'MANAGER', 'AUDITOR'] as const;

export const PAYMENT_ROUTES: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: PAYMENT_READ_ROLES },
    loadComponent: () =>
      import('./components/payment-list-page.component').then(
        (component) => component.PaymentListPageComponent,
      ),
  },
  {
    path: ':paymentId',
    canActivate: [roleGuard],
    data: { roles: PAYMENT_READ_ROLES },
    loadComponent: () =>
      import('./components/payment-detail-page.component').then(
        (component) => component.PaymentDetailPageComponent,
      ),
  },
];
