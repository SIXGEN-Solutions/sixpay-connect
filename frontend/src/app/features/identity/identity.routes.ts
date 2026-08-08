import { Routes } from '@angular/router';

import { identityGuard } from './guards/identity.guard';

export const IDENTITY_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'users' },
  {
    path: 'users',
    canActivate: [identityGuard],
    loadComponent: () =>
      import('./components/users-page.component').then((component) => component.UsersPageComponent),
  },
  {
    path: 'roles',
    canActivate: [identityGuard],
    loadComponent: () =>
      import('./components/roles-page.component').then((component) => component.RolesPageComponent),
  },
];
