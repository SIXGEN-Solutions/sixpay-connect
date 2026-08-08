import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { apiUrlInterceptor, resolveSixpayApiUrl } from './api-url.interceptor';

describe('apiUrlInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiUrlInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controller.verify();
  });

  it('accepts the Partner public API root', () => {
    http.get('/api/v1/partners').subscribe();

    const request = controller.expectOne('/api/v1/partners');
    request.flush({});
  });

  it('accepts the internal query API root', () => {
    http.get('/internal/api/v1/payments').subscribe();

    const request = controller.expectOne('/internal/api/v1/payments');
    request.flush({});
  });

  it('does not rewrite unrelated application paths', () => {
    expect(resolveSixpayApiUrl('/assets/config.json', 'https://sixpay.example')).toBe(
      '/assets/config.json',
    );
  });

  it('does not rewrite absolute URLs', () => {
    expect(
      resolveSixpayApiUrl('https://example.test/health', 'https://sixpay.example'),
    ).toBe('https://example.test/health');
  });

  it('prefixes a public API URL with apiBaseUrl', () => {
    expect(
      resolveSixpayApiUrl('/api/v1/partners', 'https://sixpay.example/'),
    ).toBe('https://sixpay.example/api/v1/partners');
  });

  it('prefixes an internal API URL with apiBaseUrl', () => {
    expect(
      resolveSixpayApiUrl('/internal/api/v1/payments', 'https://sixpay.example/'),
    ).toBe('https://sixpay.example/internal/api/v1/payments');
  });
});
