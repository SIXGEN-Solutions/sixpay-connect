import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { SixpayRole } from '../../../core/auth/authentication.model';

export const partnerRoleGuard: CanActivateFn = (route, state) => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);
  const roles = (route.data['roles'] as readonly SixpayRole[] | undefined) ?? [];

  return authentication.ready$.pipe(
    map(() => {
      if (!authentication.isAuthenticated()) {
        return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
      }
      return authentication.hasAnyRole(roles) ? true : router.createUrlTree(['/forbidden']);
    }),
  );
};
