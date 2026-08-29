import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, ErrorHandler, provideZoneChangeDetection } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { LogLevel, provideAuth } from 'angular-auth-oidc-client';

import { authenticationInterceptor } from './core/auth/authentication.interceptor';
import { sessionErrorInterceptor } from './core/auth/session-error.interceptor';
import { apiUrlInterceptor } from './core/http/api-url.interceptor';
import { correlationIdInterceptor } from './core/http/correlation-id.interceptor';
import { idempotencyKeyInterceptor } from './core/http/idempotency-key.interceptor';
import { apiErrorInterceptor } from './core/errors/api-error.interceptor';
import { GlobalErrorHandler } from './core/errors/global-error.handler';
import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { validateAuthenticationEnvironment } from '../environments/authentication-environment';
import { AuthenticationEnvironment } from '../environments/environment.model';

validateAuthenticationEnvironment(environment);

const authenticationEnvironment: AuthenticationEnvironment = environment.authentication;

const oidcProviders = authenticationEnvironment.oidc.enabled
  ? [
      provideAuth({
        config: {
          authority: authenticationEnvironment.oidc.authority!,
          clientId: authenticationEnvironment.oidc.clientId!,
          scope: authenticationEnvironment.oidc.scope!,
          responseType: 'code',
          redirectUrl: window.location.origin,
          postLogoutRedirectUri: window.location.origin,
          silentRenew: true,
          useRefreshToken: true,
          renewTimeBeforeTokenExpiresInSeconds: 30,
          unauthorizedRoute: '/login',
          forbiddenRoute: '/forbidden',
          logLevel: LogLevel.None,
        },
      }),
    ]
  : [];

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideAnimationsAsync(),
    provideRouter(routes),
    ...oidcProviders,
    provideHttpClient(
      withInterceptors([
        apiUrlInterceptor,
        correlationIdInterceptor,
        idempotencyKeyInterceptor,
        authenticationInterceptor,
        sessionErrorInterceptor,
        apiErrorInterceptor,
      ]),
    ),
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
  ],
};
