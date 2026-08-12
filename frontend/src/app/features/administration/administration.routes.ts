import { Routes } from '@angular/router';

import { roleGuard } from '../../core/auth/role.guard';

const ADMIN_ONLY = ['ADMIN'] as const;

export const ADMINISTRATION_ROUTES: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: ADMIN_ONLY },
    loadComponent: () =>
      import('./components/administration-page.component').then(
        (component) => component.AdministrationPageComponent,
      ),
  },
  {
    path: 'users',
    canActivate: [roleGuard],
    data: { roles: ADMIN_ONLY },
    loadComponent: () =>
      import('./components/security-users-page.component').then(
        (component) => component.SecurityUsersPageComponent,
      ),
  },
  {
    path: 'users/new',
    canActivate: [roleGuard],
    data: { roles: ADMIN_ONLY },
    loadComponent: () =>
      import('./components/security-user-create-page.component').then(
        (component) => component.SecurityUserCreatePageComponent,
      ),
  },
  {
    path: 'users/:userId',
    canActivate: [roleGuard],
    data: { roles: ADMIN_ONLY },
    loadComponent: () =>
      import('./components/security-user-detail-page.component').then(
        (component) => component.SecurityUserDetailPageComponent,
      ),
  },
  {
    path: 'settings',
    canActivate: [roleGuard],
    data: { roles: ADMIN_ONLY },
    loadComponent: () =>
      import('./components/settings-page.component').then(
        (component) => component.SettingsPageComponent,
      ),
  },
  {
    path: 'integrations',
    canActivate: [roleGuard],
    data: { roles: ADMIN_ONLY },
    loadComponent: () =>
      import('./components/integrations-page.component').then(
        (component) => component.IntegrationsPageComponent,
      ),
  },
];
