import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { RequestIdService } from './request-id.service';

const IDEMPOTENCY_KEY_HEADER = 'Idempotency-Key';
const MUTATION_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export const idempotencyKeyInterceptor: HttpInterceptorFn = (request, next) => {
  if (!MUTATION_METHODS.has(request.method) || request.headers.has(IDEMPOTENCY_KEY_HEADER)) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        [IDEMPOTENCY_KEY_HEADER]: inject(RequestIdService).generate(),
      },
    }),
  );
};
