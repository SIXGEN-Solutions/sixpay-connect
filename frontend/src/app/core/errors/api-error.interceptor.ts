import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { ApiProblemDetail, ApplicationError } from './api-error.model';
import { ErrorService } from './error.service';

const CORRELATION_ID_HEADER = 'X-Correlation-ID';

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const errorService = inject(ErrorService);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        errorService.publish(toApplicationError(error));
      }

      return throwError(() => error);
    }),
  );
};

function toApplicationError(response: HttpErrorResponse): ApplicationError {
  const problem =
    response.error && typeof response.error === 'object'
      ? (response.error as ApiProblemDetail)
      : {};

  return {
    status: problem.status ?? response.status,
    title: problem.title ?? 'Erreur de communication',
    detail: problem.detail ?? 'Une erreur est survenue pendant la communication avec le serveur.',
    fieldErrors: problem.errors ?? {},
    correlationId:
      response.headers.get(CORRELATION_ID_HEADER) ??
      response.headers.get(CORRELATION_ID_HEADER.toLowerCase()),
  };
}
