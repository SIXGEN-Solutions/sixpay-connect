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
  {
    path: 'unauthorized',
    redirectTo: 'forbidden',
  },
  {
    path: '**',
    redirectTo: '',
  },
];
