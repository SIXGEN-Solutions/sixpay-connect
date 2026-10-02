import { ErrorHandler, inject, Injectable } from '@angular/core';

import { ErrorService } from './error.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly errorService = inject(ErrorService);

  handleError(error: unknown): void {
    this.errorService.publish({
      kind: 'generic',
      status: 0,
      title: 'Erreur inattendue',
      detail: 'Une erreur inattendue est survenue dans l’application.',
      fieldErrors: {},
      correlationId: null,
      retryAfterSeconds: null,
    });

    // Keep the original error observable in browser diagnostics without
    // rethrowing it and without changing the application's error flow.
    console.error('[SIXPAY] Unhandled frontend error', error);
  }
}
