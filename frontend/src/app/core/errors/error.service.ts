import { Injectable, signal } from '@angular/core';

import { ApplicationError } from './api-error.model';

@Injectable({ providedIn: 'root' })
export class ErrorService {
  private readonly currentErrorState = signal<ApplicationError | null>(null);

  readonly currentError = this.currentErrorState.asReadonly();

  publish(error: ApplicationError): void {
    this.currentErrorState.set(error);
  }

  clear(): void {
    this.currentErrorState.set(null);
  }
}
