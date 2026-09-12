import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { firstValueFrom, Observable, ReplaySubject } from 'rxjs';

import { SixpayRole } from './authentication.model';
import { AuthenticationService } from './authentication.service';
import { authorizationGuard, AuthorizationRouteData } from './authorization.guard';

describe('authorizationGuard', () => {
  let ready: ReplaySubject<boolean>;
  let authenticated: boolean;
  let standalone: boolean;
  let roles: Set<SixpayRole>;
  let permissions: Set<string>;

  beforeEach(() => {
    ready = new ReplaySubject<boolean>(1);
    authenticated = true;
    standalone = false;
    roles = new Set<SixpayRole>(['ADMIN']);
    permissions = new Set<string>();

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthenticationService,
          useValue: {
            ready$: ready.asObservable(),
            isAuthenticated: () => authenticated,
            get isStandaloneMode() {
              return standalone;
            },
            hasAnyRole: (required: readonly SixpayRole[]) =>
              required.some((role) => roles.has(role)),
            hasAllPermissions: (required: readonly string[]) =>
              required.every((permission) => permissions.has(permission)),
          },
        },
      ],
    });
  });

  it('allows role-only routes when the role matches', async () => {
    const resultPromise = evaluate({ roles: ['ADMIN'] }, '/administration');
    ready.next(true);
    expect(await resultPromise).toBe(true);
  });

  it('allows a permission-aware route when both role and permission match', async () => {
    roles = new Set<SixpayRole>(['AUDITOR']);
    permissions = new Set<string>(['payment.audit.read']);

    const resultPromise = evaluate(
      { roles: ['AUDITOR'], permissions: ['payment.audit.read'] },
      '/reporting',
    );
    ready.next(true);

    expect(await resultPromise).toBe(true);
  });

  it('rejects a permission-aware route when permission is missing', async () => {
    roles = new Set<SixpayRole>(['AUDITOR']);
    const router = TestBed.inject(Router);

    const resultPromise = evaluate(
      { roles: ['AUDITOR'], permissions: ['payment.audit.read'] },
      '/reporting',
    );
    ready.next(true);

    const result = await resultPromise;
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/forbidden');
  });

  it('preserves the explicit standalone role fallback', async () => {
    standalone = true;
    roles = new Set<SixpayRole>(['MANAGER']);

    const resultPromise = evaluate(
      {
        roles: ['ADMIN', 'MANAGER', 'AUDITOR'],
        permissions: ['payment.read'],
        standaloneRoles: ['ADMIN', 'MANAGER', 'AUDITOR'],
      },
      '/payments',
    );
    ready.next(true);

    expect(await resultPromise).toBe(true);
  });

  it('does not use role fallback outside standalone mode', async () => {
    standalone = false;
    roles = new Set<SixpayRole>(['MANAGER']);
    const router = TestBed.inject(Router);

    const resultPromise = evaluate(
      {
        roles: ['ADMIN', 'MANAGER', 'AUDITOR'],
        permissions: ['payment.read'],
        standaloneRoles: ['ADMIN', 'MANAGER', 'AUDITOR'],
      },
      '/payments/PAY-1',
    );
    ready.next(true);

    const result = await resultPromise;
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/forbidden');
  });

  it('redirects anonymous deep links to login with returnUrl', async () => {
    authenticated = false;
    const router = TestBed.inject(Router);

    const resultPromise = evaluate(
      { roles: ['AUDITOR'], permissions: ['payment.audit.read'] },
      '/reporting/payments/PAY-1/timeline',
    );
    ready.next(true);

    const result = await resultPromise;
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe(
      '/login?returnUrl=%2Freporting%2Fpayments%2FPAY-1%2Ftimeline',
    );
  });

  function evaluate(data: AuthorizationRouteData, url: string): Promise<boolean | UrlTree> {
    return TestBed.runInInjectionContext(() => {
      const route = { data } as unknown as ActivatedRouteSnapshot;
      const state = { url } as RouterStateSnapshot;
      const result = authorizationGuard(route, state) as Observable<boolean | UrlTree>;
      return firstValueFrom(result);
    });
  }
});
