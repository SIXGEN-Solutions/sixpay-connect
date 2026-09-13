import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { SixpayRole } from './authentication.model';
import { AuthenticationService } from './authentication.service';

export interface AuthorizationRouteData {
  readonly roles?: readonly SixpayRole[];
  readonly permissions?: readonly string[];
  readonly standaloneRoles?: readonly SixpayRole[];
}

/**
 * Frontend route authorization aligned with the backend-authoritative session.
 *
 * This guard only controls UX/navigation. Backend authorization remains
 * authoritative for every API request.
 *
 * In standalone mode no canonical permissions are issued. Explicit
 * standaloneRoles preserve the existing local role simulator behaviour without
 * granting such a fallback in API-backed modes.
 */
export const authorizationGuard: CanActivateFn = (route, state) => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);
  const data = route.data as AuthorizationRouteData;
  const roles = data.roles ?? [];
  const permissions = data.permissions ?? [];
  const standaloneRoles = data.standaloneRoles ?? roles;

  return authentication.ready$.pipe(
    map(() => {
      if (!authentication.isAuthenticated()) {
        return router.createUrlTree(['/login'], {
          queryParams: { returnUrl: state.url },
        });
      }

      if (roles.length > 0 && !authentication.hasAnyRole(roles)) {
        return router.createUrlTree(['/forbidden']);
      }

      if (permissions.length === 0) {
        return true;
      }

      if (authentication.hasAllPermissions(permissions)) {
        return true;
      }

      if (
        authentication.isStandaloneMode &&
        standaloneRoles.length > 0 &&
        authentication.hasAnyRole(standaloneRoles)
      ) {
        return true;
      }

      return router.createUrlTree(['/forbidden']);
    }),
  );
};
