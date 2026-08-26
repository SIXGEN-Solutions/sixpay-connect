import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import {
  AuthenticationSessionResponse,
  extractSixpayRoles,
} from './authentication.model';
import { AuthenticationService } from './authentication.service';
import { LocalAuthenticationClient } from './local-authentication.client';

describe('AuthenticationService', () => {
  beforeEach(() => {
    sessionStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('initializes the explicitly configured standalone identity', () => {
    const authentication =
      TestBed.inject(AuthenticationService);

    expect(authentication.isAuthenticated()).toBe(true);
    expect(authentication.subject()).toBe(
      'local-security-user',
    );
    expect(
      authentication.hasAnyRole([
        'ADMIN',
        'MANAGER',
        'AUDITOR',
      ]),
    ).toBe(true);
    expect(
      authentication.hasRole('PARTNER'),
    ).toBe(false);
  });

  it('uses the configured standalone Partner identity when PARTNER is simulated', () => {
    const authentication =
      TestBed.inject(AuthenticationService);

    authentication.simulateStandaloneRole(
      'PARTNER',
    );

    expect(
      authentication.hasRole('PARTNER'),
    ).toBe(true);
    expect(authentication.subject()).toBe(
      '11111111-1111-4111-8111-111111111111',
    );
  });

  it('switches back to the configured standalone user for internal roles', () => {
    const authentication =
      TestBed.inject(AuthenticationService);

    authentication.simulateStandaloneRole(
      'PARTNER',
    );
    authentication.simulateStandaloneRole(
      'MANAGER',
    );

    expect(
      authentication.hasRole('MANAGER'),
    ).toBe(true);
    expect(authentication.subject()).toBe(
      'local-security-user',
    );
  });

  it('normalizes supported roles without retaining unrelated authorities', () => {
    const roles = extractSixpayRoles({
      roles: ['admin'],
      authorities: [
        'ROLE_AUDITOR',
        'SCOPE_partner.read',
      ],
      realm_access: {
        roles: ['manager'],
      },
    });

    expect([...roles]).toEqual([
      'ADMIN',
      'AUDITOR',
      'MANAGER',
    ]);
  });

  it('navigates mandatory password change directly to dashboard after backend promotion', () => {
    const authentication =
      TestBed.inject(AuthenticationService);

    const client =
      TestBed.inject(LocalAuthenticationClient);

    const router =
      TestBed.inject(Router);

    const navigateByUrl =
      vi.spyOn(
        router,
        'navigateByUrl',
      ).mockResolvedValue(true);

    vi.spyOn(
      client,
      'changePassword',
    ).mockReturnValue(
      of(undefined),
    );

    const promotedSession:
      AuthenticationSessionResponse = {
        authenticated: true,
        subject:
          'f0383c4b-3d32-3446-81bd-3d45ee0e6721',
        username: 'manager',
        roles: ['MANAGER'],
        permissions: [
          'payment.read',
          'payment.write',
        ],
        authenticationMethod: 'LOCAL',
        passwordChangeRequired: false,
      };

    vi.spyOn(
      client,
      'currentUser',
    ).mockReturnValue(
      of(promotedSession),
    );

    const internals =
      authentication as unknown as {
        activeAuthenticationMethodState: {
          set(value: 'local'): void;
        };
        passwordChangeRequiredState: {
          set(value: boolean): void;
        };
      };

    internals
      .activeAuthenticationMethodState
      .set('local');

    internals
      .passwordChangeRequiredState
      .set(true);

    sessionStorage.setItem(
      'sixpay.authentication.return-url',
      '/change-password',
    );

    let completed = false;

    authentication
      .changeLocalPassword({
        currentPassword:
          'Temporary-password-2026',
        newPassword:
          'Manager-new-password-2027',
      })
      .subscribe({
        complete: () => {
          completed = true;
        },
      });

    expect(completed).toBe(true);

    expect(
      authentication
        .passwordChangeRequired(),
    ).toBe(false);

    expect(
      authentication.username(),
    ).toBe('manager');

    expect(
      navigateByUrl,
    ).toHaveBeenCalledOnce();

    expect(
      navigateByUrl,
    ).toHaveBeenCalledWith('/');

    expect(
      sessionStorage.getItem(
        'sixpay.authentication.return-url',
      ),
    ).toBeNull();
  });

  it('does not reuse authentication routes as post-login destinations', () => {
    const authentication =
      TestBed.inject(AuthenticationService);

    const router =
      TestBed.inject(Router);

    const navigateByUrl =
      vi.spyOn(
        router,
        'navigateByUrl',
      ).mockResolvedValue(true);

    const internals =
      authentication as unknown as {
        identityState: {
          set(value: {
            subject: string;
            roles: Set<'ADMIN'>;
            permissions: Set<string>;
          }): void;
        };
        activeAuthenticationMethodState: {
          set(value: 'local'): void;
        };
        passwordChangeRequiredState: {
          set(value: boolean): void;
        };
      };

    internals.identityState.set({
      subject:
        '64f48bc3-df43-3fe2-b6d5-b608db595850',
      roles: new Set(['ADMIN']),
      permissions: new Set<string>(),
    });

    internals
      .activeAuthenticationMethodState
      .set('local');

    internals
      .passwordChangeRequiredState
      .set(false);

    sessionStorage.setItem(
      'sixpay.authentication.return-url',
      '/change-password',
    );

    authentication
      .completeLoginNavigation();

    expect(
      navigateByUrl,
    ).toHaveBeenCalledWith('/');

    expect(
      sessionStorage.getItem(
        'sixpay.authentication.return-url',
      ),
    ).toBeNull();
  });
});
