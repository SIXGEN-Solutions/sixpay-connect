import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
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
    ],
  },
  {
    path: 'unauthorized',
    loadComponent: () =>
      import('./core/auth/unauthorized.component').then(
        (component) => component.UnauthorizedComponent,
      ),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
