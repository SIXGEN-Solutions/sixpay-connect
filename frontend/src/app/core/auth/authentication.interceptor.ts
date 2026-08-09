import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { switchMap, take } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthenticationEnvironment } from '../../../environments/environment.model';

const authenticationEnvironment: AuthenticationEnvironment =
  environment.authentication;

export const authenticationInterceptor: HttpInterceptorFn = (request, next) => {
  if (
    authenticationEnvironment.mode !== 'oidc' ||
    request.headers.has('Authorization')
  ) {
    return next(request);
  }

  const oidc = inject(OidcSecurityService, { optional: true });

  if (!oidc) {
    return next(request);
  }

  return oidc.getAccessToken().pipe(
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