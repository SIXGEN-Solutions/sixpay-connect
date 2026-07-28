import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthenticationService } from '../auth/authentication.service';
import { authenticationInterceptor } from '../auth/authentication.interceptor';
import { correlationIdInterceptor } from './correlation-id.interceptor';
import { idempotencyKeyInterceptor } from './idempotency-key.interceptor';

describe('HTTP foundation interceptors', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(
          withInterceptors([
            correlationIdInterceptor,
            idempotencyKeyInterceptor,
            authenticationInterceptor,
          ]),
        ),
        provideHttpClientTesting(),
      ],
    });

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    TestBed.inject(AuthenticationService).clearSession();
    httpTesting.verify();
  });

  it('propagates a bearer token and a correlation identifier', () => {
    TestBed.inject(AuthenticationService).setAccessToken('header.payload.signature');
    const http = TestBed.inject(HttpClient);

    http.get('/api/v1/partners/partner-id').subscribe();

    const request = httpTesting.expectOne('/api/v1/partners/partner-id');
    expect(request.request.headers.get('Authorization')).toBe('Bearer header.payload.signature');
    expect(request.request.headers.get('X-Correlation-ID')).toMatch(/^[0-9a-f-]{36}$/i);
    request.flush({});
  });

  it('adds an idempotency key only to mutation requests', () => {
    const http = TestBed.inject(HttpClient);

    http.post('/api/v1/partners', {}).subscribe();
    const mutation = httpTesting.expectOne('/api/v1/partners');
    expect(mutation.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/i);
    mutation.flush({});

    http.get('/api/v1/partners/partner-id').subscribe();
    const query = httpTesting.expectOne('/api/v1/partners/partner-id');
    expect(query.request.headers.has('Idempotency-Key')).toBe(false);
    query.flush({});
  });
});
