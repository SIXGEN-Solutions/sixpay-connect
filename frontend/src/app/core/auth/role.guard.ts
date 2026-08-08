import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { SixpayRole } from './authentication.model';
import { AuthenticationService } from './authentication.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);
  const roles = (route.data['roles'] as readonly SixpayRole[] | undefined) ?? [];

  return authentication.ready$.pipe(
    map(() => {
      if (!authentication.isAuthenticated()) {
        return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
      }

      if (roles.length === 0) {
        return true;
      }

      return authentication.hasAnyRole(roles) ? true : router.createUrlTree(['/forbidden']);
    }),
  );
};
