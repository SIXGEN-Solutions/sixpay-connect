import { Routes } from '@angular/router';

import { roleGuard } from '../../core/auth/role.guard';

const AUDIT_ROLES = ['AUDITOR'] as const;

export const REPORTING_ROUTES: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: AUDIT_ROLES },
    loadComponent: () =>
      import('./components/reporting-home-page.component').then(
        (component) => component.ReportingHomePageComponent,
      ),
  },
  {
    path: 'payments/:paymentId/timeline',
    canActivate: [roleGuard],
    data: { roles: AUDIT_ROLES },
    loadComponent: () =>
      import('./components/payment-timeline-page.component').then(
        (component) => component.PaymentTimelinePageComponent,
      ),
  },
  {
    path: 'audit-records',
    canActivate: [roleGuard],
    data: { roles: AUDIT_ROLES },
    loadComponent: () =>
      import('./components/payment-audit-list-page.component').then(
        (component) => component.PaymentAuditListPageComponent,
      ),
  },
  {
    path: 'audit-records/:auditId',
    canActivate: [roleGuard],
    data: { roles: AUDIT_ROLES },
    loadComponent: () =>
      import('./components/payment-audit-detail-page.component').then(
        (component) => component.PaymentAuditDetailPageComponent,
      ),
  },
  {
    path: 'exports',
    canActivate: [roleGuard],
    data: { roles: AUDIT_ROLES },
    loadComponent: () =>
      import('./components/payment-audit-export-page.component').then(
        (component) => component.PaymentAuditExportPageComponent,
      ),
  },
  {
    path: 'exports/:exportId',
    canActivate: [roleGuard],
    data: { roles: AUDIT_ROLES },
    loadComponent: () =>
      import('./components/payment-audit-export-status-page.component').then(
        (component) => component.PaymentAuditExportStatusPageComponent,
      ),
  },
];
