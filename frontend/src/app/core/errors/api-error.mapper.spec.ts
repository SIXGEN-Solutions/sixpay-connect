import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';

import { mapHttpErrorResponse } from './api-error.interceptor';

describe('API error mapping', () => {
  it('maps RFC 7807 details and field errors for forms', () => {
    const response = new HttpErrorResponse({
      status: 400,
      headers: new HttpHeaders({ 'X-Correlation-ID': 'corr-123' }),
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
      status: 400,
      title: 'Invalid request',
      detail: 'Request contains invalid data',
      fieldErrors: {
        legalName: 'must not be blank',
        technicalContactEmail: 'must be a valid email',
      },
      correlationId: 'corr-123',
    });
  });

  it('uses a stable fallback when the backend does not return ProblemDetail', () => {
    const response = new HttpErrorResponse({ status: 401, error: null });

    expect(mapHttpErrorResponse(response)).toEqual({
      status: 401,
      title: 'Erreur de communication',
      detail: 'Une erreur est survenue pendant la communication avec le serveur.',
      fieldErrors: {},
      correlationId: null,
    });
  });
});
