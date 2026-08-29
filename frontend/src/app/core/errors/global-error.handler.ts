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

    // On évite de relancer l'exception ici pour ne pas
    // provoquer une boucle d'erreurs globales.
    void error;
  }
}
