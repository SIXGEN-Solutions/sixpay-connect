import { Routes } from '@angular/router';

import { authenticationGuard } from './core/auth/authentication.guard';

export const routes: Routes = [
  {
    path: '',
    canActivate: [authenticationGuard],
    loadComponent: () =>
      import('./layout/shell/shell.component').then((component) => component.ShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () =>
          import('./features/dashboard/components/dashboard-page.component').then(
            (component) => component.DashboardPageComponent,
          ),
      },
      {
        path: 'design-system',
        loadComponent: () =>
          import('./features/dashboard/components/design-system-catalog.component').then(
            (component) => component.DesignSystemCatalogComponent,
          ),
      },
      {
        path: 'partners',
        loadChildren: () =>
          import('./features/partners/partners.routes').then((routes) => routes.PARTNER_ROUTES),
      },
      {
        path: 'payments',
        loadChildren: () =>
          import('./features/payments/payments.routes').then((routes) => routes.PAYMENT_ROUTES),
      },
      {
        path: 'reporting',
        loadChildren: () =>
          import('./features/reporting/reporting.routes').then(
            (routes) => routes.REPORTING_ROUTES,
          ),
      },
      {
        path: 'customers',
        loadChildren: () =>
          import('./features/customers/customers.routes').then((routes) => routes.CUSTOMER_ROUTES),
      },
      {
        path: 'accounting',
        loadChildren: () =>
          import('./features/accounting/accounting.routes').then((routes) => routes.ACCOUNTING_ROUTES),
      },
      {
        path: 'incidents',
        loadChildren: () =>
          import('./features/incidents/incidents.routes').then((routes) => routes.INCIDENT_ROUTES),
      },
      {
        path: 'administration',
        loadChildren: () =>
          import('./features/administration/administration.routes').then(
            (routes) => routes.ADMINISTRATION_ROUTES,
          ),
      },
      {
        path: 'identity',
        loadChildren: () =>
          import('./features/identity/identity.routes').then((routes) => routes.IDENTITY_ROUTES),
      },
    ],
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./core/auth/login.component').then((component) => component.LoginComponent),
  },
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./core/auth/unauthorized.component').then(
        (component) => component.ForbiddenComponent,
      ),
  },
  { path: 'unauthorized', redirectTo: 'forbidden' },
  { path: '**', redirectTo: '' },
];
