import { Routes } from '@angular/router';

import { administrationGuard } from './guards/administration.guard';

export const ADMINISTRATION_ROUTES: Routes = [
  {
    path: '',
    canActivate: [administrationGuard],
    loadComponent: () =>
      import('./components/administration-page.component').then(
        (component) => component.AdministrationPageComponent,
      ),
  },
  {
    path: 'settings',
    canActivate: [administrationGuard],
    loadComponent: () =>
      import('./components/settings-page.component').then(
        (component) => component.SettingsPageComponent,
      ),
  },
  {
    path: 'integrations',
    canActivate: [administrationGuard],
    loadComponent: () =>
      import('./components/integrations-page.component').then(
        (component) => component.IntegrationsPageComponent,
      ),
  },
];
