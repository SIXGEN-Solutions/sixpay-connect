import { HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthenticationService } from './authentication.service';
import { sessionErrorInterceptor } from './session-error.interceptor';

describe('sessionErrorInterceptor', () => {
  it('does not treat local login 401 as an expired session', async () => {
    const authentication = {
      expireSession: vi.fn(),
    };
    const router = {
      url: '/login',
      navigate: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthenticationService, useValue: authentication },
        { provide: Router, useValue: router },
      ],
    });

    const request = new HttpRequest('POST', '/api/v1/auth/login', {});

    await expect(
      firstValueFrom(
        TestBed.runInInjectionContext(() =>
          sessionErrorInterceptor(request, () =>
            throwError(
              () =>
                new HttpErrorResponse({
                  status: 401,
                }),
            ),
          ),
        ),
      ),
    ).rejects.toBeInstanceOf(HttpErrorResponse);

    expect(authentication.expireSession).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('expires a business session on 401', async () => {
    const authentication = {
      expireSession: vi.fn(),
    };
    const router = {
      url: '/payments',
      navigate: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthenticationService, useValue: authentication },
        { provide: Router, useValue: router },
      ],
    });

    const request = new HttpRequest('GET', '/internal/api/v1/payments');

    await expect(
      firstValueFrom(
        TestBed.runInInjectionContext(() =>
          sessionErrorInterceptor(request, () =>
            throwError(
              () =>
                new HttpErrorResponse({
                  status: 401,
                }),
            ),
          ),
        ),
      ),
    ).rejects.toBeInstanceOf(HttpErrorResponse);

    expect(authentication.expireSession).toHaveBeenCalledOnce();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: {
        returnUrl: '/payments',
        sessionExpired: true,
      },
    });
  });
});
