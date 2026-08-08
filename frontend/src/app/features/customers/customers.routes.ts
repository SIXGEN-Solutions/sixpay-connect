import { Routes } from '@angular/router';

import { customersGuard } from './guards/customers.guard';

export const CUSTOMER_ROUTES: Routes = [
  {
    path: '',
    canActivate: [customersGuard],
    loadComponent: () =>
      import('./components/customer-list-page.component').then(
        (component) => component.CustomerListPageComponent,
      ),
  },
  {
    path: ':observedCustomerId',
    canActivate: [customersGuard],
    loadComponent: () =>
      import('./components/customer-detail-page.component').then(
        (component) => component.CustomerDetailPageComponent,
      ),
  },
];
