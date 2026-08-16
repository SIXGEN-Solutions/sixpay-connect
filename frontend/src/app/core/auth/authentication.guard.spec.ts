import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { firstValueFrom, of } from 'rxjs';

import { authenticationGuard } from './authentication.guard';
import { AuthenticationService } from './authentication.service';

describe('authenticationGuard', () => {
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

    const result =
      await TestBed.runInInjectionContext(
        () =>
          firstValueFrom(
            authenticationGuard(
              {} as ActivatedRouteSnapshot,
              {
                url: '/payments',
              } as RouterStateSnapshot,
            ) as any,
          ),
      );

    const router =
      TestBed.inject(Router);

    expect(result instanceof UrlTree)
      .toBe(true);

    expect(
      router.serializeUrl(
        result as UrlTree,
      ),
    ).toBe('/change-password');
  });
});
