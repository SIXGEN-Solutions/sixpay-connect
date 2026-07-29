import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthenticationService } from './authentication.service';

export const authenticationGuard: CanActivateFn = (_route, state) => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);

  return authentication.ready$.pipe(
    map(() =>
      authentication.isAuthenticated()
        ? true
        : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } }),
    ),
  );
};
