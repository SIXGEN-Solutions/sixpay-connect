import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthenticationService } from './authentication.service';
import { sessionErrorInterceptor } from './session-error.interceptor';

describe('sessionErrorInterceptor', () => {
  const authentication = { expireSession: vi.fn() };
  const router = { url: '/partners', navigate: vi.fn().mockResolvedValue(true) };
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    authentication.expireSession.mockClear();
    router.navigate.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([sessionErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthenticationService, useValue: authentication },
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('clears the session and redirects to login on 401', () => {
    http.get('/api/v1/partners/id').subscribe({ error: () => undefined });
    httpTesting
      .expectOne('/api/v1/partners/id')
      .flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(authentication.expireSession).toHaveBeenCalledOnce();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/partners', sessionExpired: true },
    });
  });

  it('keeps the session and redirects to forbidden on 403', () => {
    http.get('/api/v1/partners/id').subscribe({ error: () => undefined });
    httpTesting
      .expectOne('/api/v1/partners/id')
      .flush({}, { status: 403, statusText: 'Forbidden' });

    expect(authentication.expireSession).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/forbidden']);
  });
});
