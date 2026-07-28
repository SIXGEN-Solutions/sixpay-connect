import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { RequestIdService } from './request-id.service';

const CORRELATION_ID_HEADER = 'X-Correlation-ID';

export const correlationIdInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.headers.has(CORRELATION_ID_HEADER)) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        [CORRELATION_ID_HEADER]: inject(RequestIdService).generate(),
      },
    }),
  );
};
