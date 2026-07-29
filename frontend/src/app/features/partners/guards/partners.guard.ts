import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthenticationService } from '../../../core/auth/authentication.service';

export const partnerRoleGuard: CanActivateFn = (route) => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);
  const roles = (route.data['roles'] as readonly string[] | undefined) ?? [];

  return authentication.isAuthenticated() && roles.some((role) => authentication.hasRole(role))
    ? true
    : router.createUrlTree(['/unauthorized']);
};
