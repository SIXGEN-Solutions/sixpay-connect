import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthenticationService } from './authentication.service';

export const sessionErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        authentication.expireSession();
        void router.navigate(['/login'], {
          queryParams: { returnUrl: router.url, sessionExpired: true },
        });
      } else if (error instanceof HttpErrorResponse && error.status === 403) {
        void router.navigate(['/forbidden']);
      }
      return throwError(() => error);
    }),
  );
};
