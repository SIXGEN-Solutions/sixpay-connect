import { Routes } from '@angular/router';

import { authorizationGuard } from '../../core/auth/authorization.guard';

const AUDIT_ROLES = ['AUDITOR'] as const;
const AUDIT_READ = ['payment.audit.read'] as const;
const AUDIT_EXPORT = ['payment.audit.read', 'payment.audit.export'] as const;

export const REPORTING_ROUTES: Routes = [
  {
    path: '',
    canActivate: [authorizationGuard],
    data: { roles: AUDIT_ROLES, permissions: AUDIT_READ, standaloneRoles: AUDIT_ROLES },
    loadComponent: () =>
      import('./components/reporting-home-page.component').then(
        (component) => component.ReportingHomePageComponent,
      ),
  },
  {
    path: 'payments/:paymentId/timeline',
    canActivate: [authorizationGuard],
    data: { roles: AUDIT_ROLES, permissions: AUDIT_READ, standaloneRoles: AUDIT_ROLES },
    loadComponent: () =>
      import('./components/payment-timeline-page.component').then(
        (component) => component.PaymentTimelinePageComponent,
      ),
  },
  {
    path: 'audit-records',
    canActivate: [authorizationGuard],
    data: { roles: AUDIT_ROLES, permissions: AUDIT_READ, standaloneRoles: AUDIT_ROLES },
    loadComponent: () =>
      import('./components/payment-audit-list-page.component').then(
        (component) => component.PaymentAuditListPageComponent,
      ),
  },
  {
    path: 'audit-records/:auditId',
    canActivate: [authorizationGuard],
    data: { roles: AUDIT_ROLES, permissions: AUDIT_READ, standaloneRoles: AUDIT_ROLES },
    loadComponent: () =>
      import('./components/payment-audit-detail-page.component').then(
        (component) => component.PaymentAuditDetailPageComponent,
      ),
  },
  {
    path: 'exports',
    canActivate: [authorizationGuard],
    data: { roles: AUDIT_ROLES, permissions: AUDIT_EXPORT, standaloneRoles: AUDIT_ROLES },
    loadComponent: () =>
      import('./components/payment-audit-export-page.component').then(
        (component) => component.PaymentAuditExportPageComponent,
      ),
  },
  {
    path: 'exports/:exportId',
    canActivate: [authorizationGuard],
    data: { roles: AUDIT_ROLES, permissions: AUDIT_EXPORT, standaloneRoles: AUDIT_ROLES },
    loadComponent: () =>
      import('./components/payment-audit-export-status-page.component').then(
        (component) => component.PaymentAuditExportStatusPageComponent,
      ),
  },
];
