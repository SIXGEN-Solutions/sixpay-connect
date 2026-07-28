import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthenticationService } from './authentication.service';

export const authenticationGuard: CanActivateFn = () => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);

  return authentication.isAuthenticated() ? true : router.createUrlTree(['/unauthorized']);
};
