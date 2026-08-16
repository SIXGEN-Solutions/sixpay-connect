import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthenticationService } from './authentication.service';

/**
 * The LOCAL password endpoint requires a backend-authenticated LOCAL session.
 *
 * A normal LOCAL user may also open the page voluntarily; DA-10.6 only makes
 * the route mandatory when passwordChangeRequired=true.
 */
export const localPasswordChangeGuard: CanActivateFn = () => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);

  return authentication.ready$.pipe(
    map(() => {
      if (!authentication.isAuthenticated()) {
        return router.createUrlTree(
          ['/login'],
          {
            queryParams: {
              returnUrl: '/change-password',
            },
          },
        );
      }

      if (
        authentication.activeAuthenticationMethod() !== 'local'
      ) {
        return router.createUrlTree(['/']);
      }

      return true;
    }),
  );
};
