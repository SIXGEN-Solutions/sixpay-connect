import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthenticationService } from './authentication.service';

/**
 * Protects the application shell.
 *
 * DA-10.6 treats a restricted LOCAL session as authenticated but prevents it
 * from entering business routes until the password lifecycle is remediated.
 */
export const authenticationGuard: CanActivateFn = (_route, state) => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);

  return authentication.ready$.pipe(
    map(() => {
      if (!authentication.isAuthenticated()) {
        return router.createUrlTree(['/login'], {
          queryParams: {
            returnUrl: state.url,
          },
        });
      }

      if (
        authentication.activeAuthenticationMethod() === 'local' &&
        authentication.passwordChangeRequired()
      ) {
        return router.createUrlTree(['/change-password']);
      }

      return true;
    }),
  );
};
