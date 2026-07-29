import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap, take } from 'rxjs';

import { AuthenticationService } from './authentication.service';

export const authenticationInterceptor: HttpInterceptorFn = (request, next) => {
  const authentication = inject(AuthenticationService);

  if (request.headers.has('Authorization')) {
    return next(request);
  }

  return authentication.accessTokenForRequest().pipe(
    take(1),
    switchMap((accessToken) =>
      next(
        accessToken
          ? request.clone({
              setHeaders: {
                Authorization: `Bearer ${accessToken}`,
              },
            })
          : request,
      ),
    ),
  );
};
