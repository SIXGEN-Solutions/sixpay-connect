import { HttpInterceptorFn } from '@angular/common/http';

import { environment } from '../../../environments/environment';

const ABSOLUTE_URL_PATTERN = /^https?:\/\//i;

export const apiUrlInterceptor: HttpInterceptorFn = (request, next) => {
  if (ABSOLUTE_URL_PATTERN.test(request.url) || !request.url.startsWith('/api/')) {
    return next(request);
  }

  return next(
    request.clone({
      url: `${environment.apiBaseUrl}${request.url}`,
    }),
  );
};
