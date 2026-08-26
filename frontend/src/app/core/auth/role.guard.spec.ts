import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { firstValueFrom, Observable, ReplaySubject } from 'rxjs';

import { SixpayRole } from './authentication.model';
import { AuthenticationService } from './authentication.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  let ready: ReplaySubject<boolean>;
  let authenticated: boolean;
  let roles: Set<SixpayRole>;

  beforeEach(() => {
    ready = new ReplaySubject<boolean>(1);
    authenticated = true;
    roles = new Set<SixpayRole>(['ADMIN']);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthenticationService,
          useValue: {
            ready$: ready.asObservable(),
            isAuthenticated: () => authenticated,
            hasAnyRole: (required: readonly SixpayRole[]) =>
              required.some((role) => roles.has(role)),
          },
        },
      ],
    });
  });

  it('allows an authenticated user with a required role', async () => {
    const resultPromise = evaluate(['ADMIN'], '/administration');
    ready.next(true);

    expect(await resultPromise).toBe(true);
  });

  it('redirects an authenticated user without the required role', async () => {
    roles = new Set<SixpayRole>(['PARTNER']);
    const router = TestBed.inject(Router);
    const resultPromise = evaluate(['ADMIN'], '/administration');
    ready.next(true);

    const result = await resultPromise;
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/forbidden');
  });

  it('redirects an unauthenticated user to login with the returnUrl', async () => {
    authenticated = false;
    const router = TestBed.inject(Router);
    const resultPromise = evaluate(['ADMIN', 'MANAGER'], '/payments/PAY-1');
    ready.next(false);

    const result = await resultPromise;
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe(
      '/login?returnUrl=%2Fpayments%2FPAY-1',
    );
  });

  it('allows an authenticated user when no role is required', async () => {
    roles = new Set<SixpayRole>(['PARTNER']);
    const resultPromise = evaluate([], '/');
    ready.next(true);

    expect(await resultPromise).toBe(true);
  });

  function evaluate(requiredRoles: readonly SixpayRole[], url: string): Promise<boolean | UrlTree> {
    return TestBed.runInInjectionContext(() => {
      const route = { data: { roles: requiredRoles } } as unknown as ActivatedRouteSnapshot;
      const state = { url } as RouterStateSnapshot;
      const result = roleGuard(route, state) as Observable<boolean | UrlTree>;

      return firstValueFrom(result);
    });
  }
});
