import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { ApplicationError, isProblemDetail } from './api-error.model';
import { ErrorService } from './error.service';

const CORRELATION_ID_HEADER = 'X-Correlation-ID';
const LOCAL_AUTH_PATH_PREFIX = '/api/v1/auth/';

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const errorService = inject(ErrorService);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        !isExpectedLocalAuthentication401(request.url, error)
      ) {
        errorService.publish(mapHttpErrorResponse(error));
      }

      return throwError(() => error);
    }),
  );
};

export function mapHttpErrorResponse(
  response: HttpErrorResponse,
): ApplicationError {
  const problem = isProblemDetail(response.error) ? response.error : null;

  return {
    status: problem?.status ?? response.status,
    title: problem?.title ?? 'Erreur de communication',
    detail:
      problem?.detail ??
      'Une erreur est survenue pendant la communication avec le serveur.',
    fieldErrors: problem?.errors ?? {},
    correlationId:
      response.headers.get(CORRELATION_ID_HEADER) ??
      response.headers.get(CORRELATION_ID_HEADER.toLowerCase()),
  };
}

function isExpectedLocalAuthentication401(
  url: string,
  error: HttpErrorResponse,
): boolean {
  return (
    error.status === 401 &&
    (url.startsWith(LOCAL_AUTH_PATH_PREFIX) ||
      url.includes(LOCAL_AUTH_PATH_PREFIX))
  );
}
