import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthenticationService } from './authentication.service';
import { SixpayRole } from './authentication.model';

export const permissionGuard: CanActivateFn = (route) => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);

  const permissions =
    (route.data['permissions'] as readonly string[] | undefined) ?? [];
  const fallbackRoles =
    (route.data['fallbackRoles'] as readonly SixpayRole[] | undefined) ?? [];

  const authorized =
    permissions.length === 0 ||
    permissions.some((permission) => authentication.hasPermission(permission)) ||
    (authentication.isStandaloneMode &&
      fallbackRoles.some((role) => authentication.hasRole(role)));

  return authorized ? true : router.createUrlTree(['/forbidden']);
};
