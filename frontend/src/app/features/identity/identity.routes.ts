import { Routes } from '@angular/router';

import { roleGuard } from '../../core/auth/role.guard';

const ADMIN_ONLY = ['ADMIN'] as const;

export const IDENTITY_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'users' },
  {
    path: 'users',
    canActivate: [roleGuard],
    data: { roles: ADMIN_ONLY },
    loadComponent: () =>
      import('./components/users-page.component').then((component) => component.UsersPageComponent),
  },
  {
    path: 'roles',
    canActivate: [roleGuard],
    data: { roles: ADMIN_ONLY },
    loadComponent: () =>
      import('./components/roles-page.component').then((component) => component.RolesPageComponent),
  },
];
