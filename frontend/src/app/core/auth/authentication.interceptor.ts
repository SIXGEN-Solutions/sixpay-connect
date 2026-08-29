import { HttpInterceptorFn } from '@angular/common/http';

import { environment } from '../../../environments/environment';

const SIXPAY_API_PATH_PREFIXES = ['/api/', '/internal/api/'] as const;

/**
 * DA-8 normal business/API requests use the unified SIXPAY backend session.
 * Bearer tokens are sent only explicitly by the OIDC session-exchange call.
 */
export const authenticationInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isSixpayApiRequest(request.url)) {
    return next(request);
  }

  if (request.withCredentials) {
    return next(request);
  }

  return next(
    request.clone({
      withCredentials: true,
    }),
  );
};

function isSixpayApiRequest(url: string): boolean {
  if (SIXPAY_API_PATH_PREFIXES.some((prefix) => url.startsWith(prefix))) {
    return true;
  }

  const apiBaseUrl = environment.apiBaseUrl.replace(/\/+$/, '');

  return Boolean(apiBaseUrl && url.startsWith(`${apiBaseUrl}/`));
}
