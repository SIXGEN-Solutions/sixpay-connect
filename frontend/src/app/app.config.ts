import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, ErrorHandler, provideZoneChangeDetection } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';

import { authenticationInterceptor } from './core/auth/authentication.interceptor';
import { apiUrlInterceptor } from './core/http/api-url.interceptor';
import { correlationIdInterceptor } from './core/http/correlation-id.interceptor';
import { idempotencyKeyInterceptor } from './core/http/idempotency-key.interceptor';
import { apiErrorInterceptor } from './core/errors/api-error.interceptor';
import { GlobalErrorHandler } from './core/errors/global-error.handler';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideAnimationsAsync(),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([
        apiUrlInterceptor,
        correlationIdInterceptor,
        idempotencyKeyInterceptor,
        authenticationInterceptor,
        apiErrorInterceptor,
      ]),
    ),
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
  ],
};
