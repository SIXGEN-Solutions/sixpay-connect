import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthenticationService } from '../../../core/auth/authentication.service';

export const incidentsGuard: CanActivateFn = () => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);

  return authentication.hasAnyRole(['ADMIN', 'MANAGER', 'AUDITOR'])
    ? true
    : router.createUrlTree(['/forbidden']);
};
