import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { firstValueFrom, Observable, of } from 'rxjs';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { SixpayRole } from '../../../core/auth/authentication.model';
import { partnerRoleGuard } from './partners.guard';

describe('partnerRoleGuard', () => {
  let authenticated: boolean;
  let roles: readonly SixpayRole[];

  beforeEach(() => {
    authenticated = true;
    roles = [];
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthenticationService,
          useValue: {
            ready$: of(true),
            isAuthenticated: () => authenticated,
            hasAnyRole: (expected: readonly SixpayRole[]) =>
              expected.some((role) => roles.includes(role)),
          },
        },
      ],
    });
  });

  it('allows a route when one required role is present', async () => {
    roles = ['MANAGER'];
    expect(await runGuard(['ADMIN', 'MANAGER'])).toBe(true);
  });

  it('redirects an authenticated user without the role to forbidden', async () => {
    const result = await runGuard(['ADMIN']);
    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/forbidden');
  });

  it('redirects an unauthenticated user to login with the return URL', async () => {
    authenticated = false;
    const result = await runGuard(['ADMIN']);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe(
      '/login?returnUrl=%2Fpartners%2Fcreate',
    );
  });

  async function runGuard(expectedRoles: readonly SixpayRole[]): Promise<boolean | UrlTree> {
    const route = new ActivatedRouteSnapshot();
    route.data = { roles: expectedRoles };
    const result = TestBed.runInInjectionContext(() =>
      partnerRoleGuard(route, { url: '/partners/create' } as RouterStateSnapshot),
    );
    return firstValueFrom(result as Observable<boolean | UrlTree>);
  }
});
