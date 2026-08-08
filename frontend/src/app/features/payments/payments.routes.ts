import { Routes } from '@angular/router';

import { paymentsGuard } from './guards/payments.guard';

export const PAYMENT_ROUTES: Routes = [
  {
    path: '',
    canActivate: [paymentsGuard],
    loadComponent: () =>
      import('./components/payment-list-page.component').then(
        (component) => component.PaymentListPageComponent,
      ),
  },
  {
    path: ':paymentId',
    canActivate: [paymentsGuard],
    loadComponent: () =>
      import('./components/payment-detail-page.component').then(
        (component) => component.PaymentDetailPageComponent,
      ),
  },
];
