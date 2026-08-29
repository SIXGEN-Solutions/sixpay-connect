import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';

import { mapHttpErrorResponse, parseRetryAfter } from './api-error.interceptor';

describe('API error mapping', () => {
  it('maps RFC 7807 details and field errors for forms', () => {
    const response = new HttpErrorResponse({
      status: 400,
      headers: new HttpHeaders({
        'X-Correlation-ID': 'corr-123',
      }),
      error: {
        type: 'urn:sixpay:problem:invalid-request',
        title: 'Invalid request',
        status: 400,
        detail: 'Request contains invalid data',
        instance: '/api/v1/partners',
        errors: {
          legalName: 'must not be blank',
          technicalContactEmail: 'must be a valid email',
        },
      },
    });

    expect(mapHttpErrorResponse(response)).toEqual({
      kind: 'validation',
      status: 400,
      title: 'Invalid request',
      detail: 'Request contains invalid data',
      fieldErrors: {
        legalName: 'must not be blank',
        technicalContactEmail: 'must be a valid email',
      },
      correlationId: 'corr-123',
      retryAfterSeconds: null,
    });
  });

  it('maps API 404 to a stable not-found UX', () => {
    const response = new HttpErrorResponse({
      status: 404,
      error: null,
    });

    expect(mapHttpErrorResponse(response)).toEqual({
      kind: 'not-found',
      status: 404,
      title: 'Ressource introuvable',
      detail: 'La ressource demandée n’existe pas ou n’est plus disponible.',
      fieldErrors: {},
      correlationId: null,
      retryAfterSeconds: null,
    });
  });

  it('maps 429 and Retry-After delta seconds', () => {
    const response = new HttpErrorResponse({
      status: 429,
      headers: new HttpHeaders({
        'Retry-After': '30',
        'X-Correlation-ID': 'corr-rate-limit',
      }),
      error: null,
    });

    expect(mapHttpErrorResponse(response)).toEqual({
      kind: 'rate-limit',
      status: 429,
      title: 'Trop de requêtes',
      detail: 'Le service limite temporairement les requêtes. Réessayez après le délai indiqué.',
      fieldErrors: {},
      correlationId: 'corr-rate-limit',
      retryAfterSeconds: 30,
    });
  });

  it('parses Retry-After HTTP dates', () => {
    const now = Date.parse('2026-08-09T03:00:00Z');

    expect(parseRetryAfter('Sun, 09 Aug 2026 03:00:45 GMT', now)).toBe(45);
  });

  it('maps network failures separately', () => {
    const response = new HttpErrorResponse({
      status: 0,
      error: null,
    });

    expect(mapHttpErrorResponse(response)).toEqual({
      kind: 'network',
      status: 0,
      title: 'Service indisponible',
      detail: 'Impossible de joindre le serveur SIXPAY. Vérifiez la connectivité puis réessayez.',
      fieldErrors: {},
      correlationId: null,
      retryAfterSeconds: null,
    });
  });
});
