import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { firstValueFrom, Observable, of } from 'rxjs';

import { authenticationGuard } from './authentication.guard';
import { AuthenticationService } from './authentication.service';

describe('authenticationGuard', () => {
  it('redirects anonymous user to login and preserves requested business URL', async () => {
    const authentication = {
      ready$: of(true),
      isAuthenticated: () => false,
      activeAuthenticationMethod: () => null,
      passwordChangeRequired: () => false,
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthenticationService,
          useValue: authentication,
        },
      ],
    });

    const result = await executeGuard('/payments');

    const router = TestBed.inject(Router);

    expect(result instanceof UrlTree).toBe(true);

    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fpayments');
  });

  it('redirects a restricted LOCAL session to change-password', async () => {
    const authentication = {
      ready$: of(true),
      isAuthenticated: () => true,
      activeAuthenticationMethod: () => 'local',
      passwordChangeRequired: () => true,
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthenticationService,
          useValue: authentication,
        },
      ],
    });

    const result = await executeGuard('/payments');

    const router = TestBed.inject(Router);

    expect(result instanceof UrlTree).toBe(true);

    expect(router.serializeUrl(result as UrlTree)).toBe('/change-password');
  });

  it('allows normal LOCAL session into business routes', async () => {
    const authentication = {
      ready$: of(true),
      isAuthenticated: () => true,
      activeAuthenticationMethod: () => 'local',
      passwordChangeRequired: () => false,
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthenticationService,
          useValue: authentication,
        },
      ],
    });

    expect(await executeGuard('/payments')).toBe(true);
  });

  it('does not apply SIXPAY local-password restriction to OIDC session', async () => {
    const authentication = {
      ready$: of(true),
      isAuthenticated: () => true,
      activeAuthenticationMethod: () => 'oidc',
      passwordChangeRequired: () => true,
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthenticationService,
          useValue: authentication,
        },
      ],
    });

    expect(await executeGuard('/payments')).toBe(true);
  });

  function executeGuard(url: string): Promise<boolean | UrlTree> {
    return TestBed.runInInjectionContext(() =>
      firstValueFrom(
        authenticationGuard(
          {} as ActivatedRouteSnapshot,
          {
            url,
          } as RouterStateSnapshot,
        ) as Observable<boolean | UrlTree>,
      ),
    );
  }
});
