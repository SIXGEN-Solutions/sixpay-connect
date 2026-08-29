import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthenticationService } from '../../../core/auth/authentication.service';

export const administrationGuard: CanActivateFn = () => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);

  return authentication.hasAnyRole(['ADMIN']) ? true : router.createUrlTree(['/forbidden']);
};
