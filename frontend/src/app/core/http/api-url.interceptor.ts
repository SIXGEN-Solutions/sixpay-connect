import { HttpInterceptorFn } from '@angular/common/http';

import { environment } from '../../../environments/environment';

const ABSOLUTE_URL_PATTERN = /^https?:\/\//i;
const SIXPAY_API_PATH_PREFIXES = ['/api/', '/internal/api/'] as const;

export const apiUrlInterceptor: HttpInterceptorFn = (request, next) => {
  const resolvedUrl = resolveSixpayApiUrl(request.url, environment.apiBaseUrl);

  if (resolvedUrl === request.url) {
    return next(request);
  }

  return next(request.clone({ url: resolvedUrl }));
};

export function resolveSixpayApiUrl(url: string, apiBaseUrl: string): string {
  if (ABSOLUTE_URL_PATTERN.test(url) || !isSixpayApiPath(url) || !apiBaseUrl) {
    return url;
  }

  return `${apiBaseUrl.replace(/\/+$/, '')}${url}`;
}

function isSixpayApiPath(url: string): boolean {
  return SIXPAY_API_PATH_PREFIXES.some((prefix) => url.startsWith(prefix));
}
