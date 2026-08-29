import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthenticationService } from './authentication.service';

const LOCAL_AUTH_PATH_PREFIX = '/api/v1/auth/';

export const sessionErrorInterceptor: HttpInterceptorFn = (request, next) => {
  if (isLocalAuthenticationRequest(request.url)) {
    return next(request);
  }

  const authentication = inject(AuthenticationService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        authentication.expireSession();
        void router.navigate(['/login'], {
          queryParams: {
            returnUrl: router.url,
            sessionExpired: true,
          },
        });
      } else if (
        error instanceof HttpErrorResponse &&
        error.status === 403 &&
        authentication.activeAuthenticationMethod() === 'local' &&
        authentication.passwordChangeRequired()
      ) {
        /*
         * Defense in depth for requests already in flight when a restricted
         * session is bootstrapped. Credential restriction is not a generic
         * business authorization failure.
         */
        void router.navigate(['/change-password']);
      } else if (error instanceof HttpErrorResponse && error.status === 403) {
        void router.navigate(['/forbidden']);
      }

      return throwError(() => error);
    }),
  );
};

function isLocalAuthenticationRequest(url: string): boolean {
  return url.startsWith(LOCAL_AUTH_PATH_PREFIX) || url.includes(LOCAL_AUTH_PATH_PREFIX);
}
