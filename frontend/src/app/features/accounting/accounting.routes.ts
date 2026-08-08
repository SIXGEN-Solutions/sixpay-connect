import { Routes } from '@angular/router';

import { accountingGuard } from './guards/accounting.guard';

export const ACCOUNTING_ROUTES: Routes = [
  {
    path: '',
    canActivate: [accountingGuard],
    loadComponent: () =>
      import('./components/accounting-overview-page.component').then(
        (component) => component.AccountingOverviewPageComponent,
      ),
  },
  {
    path: 'batches/:batchId',
    canActivate: [accountingGuard],
    loadComponent: () =>
      import('./components/accounting-batch-detail-page.component').then(
        (component) => component.AccountingBatchDetailPageComponent,
      ),
  },
];
