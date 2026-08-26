import {
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import {
  ApplicationError,
  ApplicationErrorKind,
  isProblemDetail,
} from './api-error.model';
import { ErrorService } from './error.service';

const CORRELATION_ID_HEADER = 'X-Correlation-ID';
const RETRY_AFTER_HEADER = 'Retry-After';
const LOCAL_AUTH_ME_PATH = '/api/v1/auth/me';
const LOCAL_AUTH_LOGOUT_PATH = '/api/v1/auth/logout';

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const errorService = inject(ErrorService);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        !isExpectedLocalAuthenticationFailure(request.url, error)
      ) {
        errorService.publish(mapHttpErrorResponse(error));
      }

      return throwError(() => error);
    }),
  );
};

export function mapHttpErrorResponse(
  response: HttpErrorResponse,
  now = Date.now(),
): ApplicationError {
  const problem = isProblemDetail(response.error) ? response.error : null;
  const defaults = defaultErrorCopy(response.status);

  return {
    kind: classifyStatus(response.status),
    status: problem?.status ?? response.status,
    title: problem?.title ?? defaults.title,
    detail: problem?.detail ?? defaults.detail,
    fieldErrors: problem?.errors ?? {},
    correlationId:
      response.headers.get(CORRELATION_ID_HEADER) ??
      response.headers.get(CORRELATION_ID_HEADER.toLowerCase()),
    retryAfterSeconds:
      response.status === 429
        ? parseRetryAfter(response.headers.get(RETRY_AFTER_HEADER), now)
        : null,
  };
}

export function parseRetryAfter(
  value: string | null,
  now = Date.now(),
): number | null {
  if (!value) {
    return null;
  }

  const seconds = Number(value);

  if (Number.isFinite(seconds) && seconds >= 0) {
    return Math.ceil(seconds);
  }

  const retryAt = Date.parse(value);

  if (Number.isNaN(retryAt)) {
    return null;
  }

  return Math.max(0, Math.ceil((retryAt - now) / 1000));
}

function isExpectedLocalAuthenticationFailure(
  url: string,
  error: HttpErrorResponse,
): boolean {
  const expectedAnonymousStatus =
    error.status === 401 || error.status === 403;

  if (!expectedAnonymousStatus) {
    return false;
  }

  return (
    matchesPath(url, LOCAL_AUTH_ME_PATH) ||
    matchesPath(url, LOCAL_AUTH_LOGOUT_PATH)
  );
}

function matchesPath(url: string, path: string): boolean {
  if (url === path || url.startsWith(`${path}?`)) {
    return true;
  }

  try {
    return new URL(url, 'http://sixpay.local').pathname === path;
  } catch {
    return url.includes(path);
  }
}

function classifyStatus(status: number): ApplicationErrorKind {
  if (status === 0) return 'network';
  if (status === 400 || status === 422) return 'validation';
  if (status === 401) return 'unauthorized';
  if (status === 403) return 'forbidden';
  if (status === 404) return 'not-found';
  if (status === 429) return 'rate-limit';
  if (status >= 500) return 'server';

  return 'generic';
}

function defaultErrorCopy(status: number): {
  readonly title: string;
  readonly detail: string;
} {
  switch (status) {
    case 0:
      return {
        title: 'Service indisponible',
        detail:
          'Impossible de joindre le serveur SIXPAY. Vérifiez la connectivité puis réessayez.',
      };

    case 404:
      return {
        title: 'Ressource introuvable',
        detail:
          'La ressource demandée n’existe pas ou n’est plus disponible.',
      };

    case 429:
      return {
        title: 'Trop de requêtes',
        detail:
          'Le service limite temporairement les requêtes. Réessayez après le délai indiqué.',
      };

    default:
      if (status >= 500) {
        return {
          title: 'Erreur serveur',
          detail:
            'Le service SIXPAY rencontre une erreur temporaire. Réessayez ultérieurement.',
        };
      }

      return {
        title: 'Erreur de communication',
        detail:
          'Une erreur est survenue pendant la communication avec le serveur.',
      };
  }
}
