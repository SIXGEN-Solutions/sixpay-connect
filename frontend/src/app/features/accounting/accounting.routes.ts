import { Routes } from '@angular/router';

import { roleGuard } from '../../core/auth/role.guard';

const ACCOUNTING_READ_ROLES = ['ADMIN', 'MANAGER', 'AUDITOR'] as const;

export const ACCOUNTING_ROUTES: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: ACCOUNTING_READ_ROLES },
    loadComponent: () =>
      import('./components/accounting-overview-page.component').then((component) => component.AccountingOverviewPageComponent),
  },
  {
    path: 'batches/:batchId',
    canActivate: [roleGuard],
    data: { roles: ACCOUNTING_READ_ROLES },
    loadComponent: () =>
      import('./components/accounting-batch-detail-page.component').then((component) => component.AccountingBatchDetailPageComponent),
  },
];
