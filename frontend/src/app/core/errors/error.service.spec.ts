import { TestBed } from '@angular/core/testing';

import { ErrorService } from './error.service';

describe('ErrorService', () => {
  it('publie puis efface une erreur applicative', () => {
    const service = TestBed.inject(ErrorService);
    const error = {
      kind: 'server' as const,
      status: 500,
      title: 'Erreur',
      detail: 'Indisponible',
      fieldErrors: {},
      correlationId: 'corr-1',
      retryAfterSeconds: null,
    };

    service.publish(error);
    expect(service.currentError()).toEqual(error);
    service.clear();
    expect(service.currentError()).toBeNull();
  });
});
