/*import { ErrorHandler, inject, Injectable } from '@angular/core';

import { ErrorService } from './error.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly errorService = inject(ErrorService);

  handleError(error: unknown): void {
    this.errorService.publish({
      status: 0,
      title: 'Erreur inattendue',
      detail: 'Une erreur inattendue est survenue dans l’application.',
      fieldErrors: {},
      correlationId: null,
    });

    void error;
  }
}*/
import { ErrorHandler, inject, Injectable } from '@angular/core';

import { ErrorService } from './error.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly errorService = inject(ErrorService);

  handleError(error: unknown): void {
    console.error('[SIXPAY] Unhandled frontend error:', error);

    this.errorService.publish({
      status: 0,
      title: 'Erreur inattendue',
      detail: 'Une erreur inattendue est survenue dans l’application.',
      fieldErrors: {},
      correlationId: null,
    });
  }
}